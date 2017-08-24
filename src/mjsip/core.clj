(ns mjsip.core
  (:require [clojure.core.async :as async]
            [clojure.spec :as s]
            [mjsip.utils :refer [child-ns]]
            [mjsip.config :refer [*config*]])
  (:import (org.zoolu.sip.provider SipProvider
                                   SipProviderListener
                                   SipStack
                                   MethodIdentifier
                                   RegisterAgent
                                   RegisterAgentListener)
           (org.zoolu.sip.dialog OptionsDialog
                                 OptionsDialogListener)
           (org.zoolu.sip.address SipURL
                                  NameAddress)
           (org.zoolu.sip.message Message
                                  MessageFactory
                                  SipMethods)
           (org.zoolu.sip.call ExtendedCall
                               ExtendedCallListener)
           (org.zoolu.net SocketAddress)
           (org.zoolu.sdp SessionDescriptor
                          SessionNameField
                          ConnectionField
                          TimeField
                          MediaField
                          OriginField
                          AttributeField)
           (java.util UUID)
           (java.net URLEncoder)))

(child-ns event)
(child-ns call)
(child-ns sip)
(child-ns client)
(child-ns registration)
(child-ns mjsip)

(defn extern-client?
  "Checks whether the SIP-client is external. External means not associated to a B2BUA."
  [client]
  (= :extern (::client/type client)))

(defn client-sip-url
  "Creates a SipURL-object from a SIP-client with the given keywords
  retrieved from the SIP-client for the specific URL-parts. The format
  of such a URL is sip:user@host:port."
  [user-part-key host-key port-key client]
  (SipURL. (str (get client user-part-key))
           (str (get client host-key))
           (get client port-key 0)))

(defn client-address
  "Creates a NameAddress-object from a SIP-client with the given
  keywords retrieved from the SIP-client for the specific
  address-parts. The format of such a address is \"display-name\"
  <sip:user@host:port>."
  [display-name-key user-part-key host-key port-key client]
  (NameAddress. (display-name-key client)
                (client-sip-url user-part-key host-key port-key client)))

(defn contact-address
  "Creates a NameAddress-object with the information from the given
  SIP-client suited for the \"Contact\"-Field of a SIP-message."
  [client]
  (client-address ::client/display-name
                  ::client/user
                  ::client/local-address
                  ::client/local-port
                  client))

(defn target-address
  "Creates a NameAddress-object with the information from the given
  SIP-client used to define a target for the RegisterAgent."
  [client]
  (client-address ::client/display-name
                  ::client/user
                  ::client/registrar-address
                  ::client/registrar-port
                  client))

(defn callee->client-sip-url
  "Creates a SipURL-object from the given SIP-client used to define a callee."
  [callee]
  (if (extern-client? callee)
    (client-sip-url ::client/call-number
                    ::client/local-address
                    ::client/local-port
                    callee)
    (client-sip-url ::client/extension
                    ::client/registrar-address
                    ::client/registrar-port
                    callee)))

(defn sip-provider
  "Creates a SipProvider-object from the given SIP-client."
  [{::client/keys [local-address local-port registrar-address type] :as client}]
  (let [provider (SipProvider. local-address
                               (or local-port 0)
                               (into-array ["udp"])
                               local-address)]
    (when-not (extern-client? client)
      (.setOutboundProxy provider (SocketAddress. registrar-address)))
    (.addSipProviderListener provider
                             (MethodIdentifier. SipMethods/OPTIONS)
                             (reify SipProviderListener
                               (onReceivedMessage [this sip-provider message]
                                 (let [response (MessageFactory/createResponse message 200 "OK" (contact-address client))]
                                   (.sendMessage sip-provider response)))))
    provider))

(defn register-agent
  "Creates a map with ::mjsip/register-agent as the key for the
  RegisterAgent-object and ::registration/events as the key for the
  core.async-event-channel from the given SIP-client."
  [{::client/keys [registrar-address local-address local-port display-name user realm password]
    ::mjsip/keys [sip-provider]
    :as client}]
  (let [events (async/chan (get-in *config* [:common :event-channel-buffer-size]))
        listener (reify RegisterAgentListener
                   (onUaRegistrationSuccess [this rclient target contact result]
                     (println "onUaRegistrationSuccess" target contact result)
                     (async/>!! events {::event/name ::event/registration-success}))
                   (onUaRegistrationFailure [this rclient target contact result]
                     (println "onUaRegistrationFailure" target contact result)
                     (async/>!! events {::event/name ::event/registration-failure})))]
    {::mjsip/register-agent (RegisterAgent. sip-provider
                                            (str (target-address client))
                                            (str (contact-address client))
                                            user
                                            realm
                                            password
                                            listener)
     ::registration/events events}))

(defn handle-event
  "Invokes the event-handler with the name event-name contained in the
  event-handlers collection with event-attrs as parameter. If the
  given event doesn't exist the default-event-handler will be
  invoked. Returns the value returned from the invoked event-handler."
  [event-handlers event-name event-attrs]
  (when-let [handle (or (get event-handlers event-name)
                        (:default event-handlers))]
    (handle (assoc event-attrs ::event/name event-name))))

(defn extended-call
  "Creates a call-object with the given event-handlers from the given
  SIP-client which will function as the caller."
  [client event-handlers]
  (let [call-uuid (UUID/randomUUID)
        handle (fn [event-name event-attrs]
                 (handle-event event-handlers
                               event-name
                               (assoc event-attrs ::call/uuid call-uuid)))
        _ (prn (::mjsip/sip-provider client))
        listener (reify ExtendedCallListener
                              (onCallIncoming [this call callee caller sdp invite]
                                (println "onCallIncoming" (.getCallIdHeader invite))
                                (handle ::event/call-incoming
                                        {::mjsip/call call
                                         ::mjsip/callee callee
                                         ::mjsip/caller caller}))
                              (onCallModifying [this call sdp invite]
                                (println "onCallModifying" (.getCallIdHeader invite))
                                (handle ::event/call-modifying
                                        {::mjsip/call call}))
                              (onCallRinging [this call resp]
                                (println "onCallRinging")
                                (handle ::event/call-ringing
                                        {::mjsip/sip-response resp
                                         ::mjsip/call call}))
                              (onCallAccepted [this call sdp resp]
                                (println "onCallAccepted")
                                (handle ::event/call-accepted
                                        {::mjsip/call call}))
                              (onCallRefused [this call reason resp]
                                (println "onCallRefused:" reason)
                                (handle ::event/call-refused
                                        {::mjsip/call call}))
                              (onCallRedirection [this call reason contact-list resp]
                                (println "onCallRedirection")
                                (handle ::event/call-redirection
                                        {::mjsip/call call}))
                              (onCallConfirmed [this call sdp ack]
                                (println "onCallConfirmed")
                                (handle ::event/call-confirmed
                                        {::mjsip/call call}))
                              (onCallTimeout [this call]
                                (println "onCallTimeout")
                                (handle ::event/call-timeout
                                        {::mjsip/call call}))
                              (onCallReInviteAccepted [this call sdp resp]
                                (println "onCallReInviteAccepted")
                                (handle ::event/call-reinvite-accepted
                                        {::mjsip/call call}))
                              (onCallReInviteRefused [this call reason resp]
                                (println "onCallReInviteRefused")
                                (handle ::event/call-reinvite-refused
                                        {::mjsip/call call}))
                              (onCallReInviteTimeout [this call]
                                (println "onCallReInviteTimeout")
                                (handle ::event/call-reinvite-timeout
                                        {::mjsip/call call}))
                              (onCallCanceling [this call cancel]
                                (println "onCallCanceling")
                                (handle ::event/call-cancelling
                                        {::mjsip/call call}))
                              (onCallClosing [this call bye]
                                (println "onCallClosing")
                                (handle ::event/call-closing
                                        {::mjsip/call call}))
                              (onCallClosed [this call resp]
                                (println "onCallClosed")
                                (handle ::event/call-closed
                                        {::mjsip/call call}))
                              (onCallTransfer [this call refer-to refered-by refer]
                                (println "onCallTransfer")
                                (handle ::event/call-transfer
                                        {::mjsip/call call}))
                              (onCallTransferAccepted [this call resp]
                                (println "onCallTransferAccepted")
                                (handle ::event/call-transfer-accepted
                                        {::mjsip/call call}))
                              (onCallTransferRefused [this call reason resp]
                                (println "onCallTransferRefused")
                                (handle ::event/call-transfer-refused
                                        {::mjsip/call call}))
                              (onCallTransferSuccess [this call notify]
                                (println "onCallTransferSuccess")
                                (handle ::event/call-transfer-success
                                        {::mjsip/call call}))
                              (onCallTransferFailure [this call reason notify]
                                (println "onCallTransferFailure")
                                (handle ::event/call-transfer-failure
                                        {::mjsip/call call})))
        call (if (extern-client? client)
               (ExtendedCall. (::mjsip/sip-provider client)
                              (str (client-address ::client/display-name
                                                   ::client/call-number
                                                   ::client/local-address
                                                   ::client/local-port
                                                   client))
                              (str (client-address ::client/display-name
                                                   ::client/call-number
                                                   ::client/local-address
                                                   ::client/local-port
                                                   client))
                              listener)
               (ExtendedCall. (::mjsip/sip-provider client)
                              (str (client-address ::client/display-name
                                                   ::client/user
                                                   ::client/registrar-address
                                                   ::client/registrar-port
                                                   client))
                              (str (client-address ::client/display-name
                                                   ::client/extension
                                                   ::client/registrar-address
                                                   ::client/registrar-port
                                                   client))
                              (::client/user client)
                              (::client/realm client)
                              (::client/password client)
                              listener))]
    {::call/uuid call-uuid
     ::mjsip/call call}))

(defn init-call-receiver
  "Initializes the given SIP-client to receive incoming calls and call
  related events. Puts the event object of a new incoming-call into
  the core.async-channel calls-chan. This event object contains
  ::call/events which is another core.async-channel provided to
  receive call related events. Returns nil."
  [client calls-chan]
  (let [call-events (async/chan (get-in *config* [:common :event-channel-buffer-size]))
        pending-call (extended-call client
                                    {::event/call-incoming
                                     (fn [event]
                                       (init-call-receiver client calls-chan)
                                       (async/>!! calls-chan (assoc event ::call/events call-events)))
                                     :default
                                     (fn [event]
                                       (async/>!! call-events event))})]
    (.listen (::mjsip/call pending-call))))

(defn client-incoming-calls
  "Initializes the given SIP-client to receive incoming calls and call
  related events. Returns a new core.async-channel used to receive
  incoming-call-events. See init-call-receiver for more details."
  [client]
  (let [calls (async/chan (get-in *config* [:common :event-channel-buffer-size]))]
    (init-call-receiver client calls)
    calls))

(defn client
  "Creates and initializes a new SIP-client object for immediate use."
  [params]
  (-> params
      (update-with assoc ::mjsip/sip-provider sip-provider)
      (update-with merge register-agent)
      (update-with assoc ::client/incoming-calls client-incoming-calls)))

(defn register
  "Registers the given SIP-client on the associated B2BUA."
  [{::mjsip/keys [register-agent]}]
  (.loopRegister register-agent
                 (get-in *config* [:common :register-ttl-s])
                 (get-in *config* [:common :register-renew-s])))

(defn minimal-sdp
  "Helper to create a minimal SDP which probably will not work if
  initialized a real media stream with the specified SDP-information."
  []
  (doto (SessionDescriptor. (OriginField. "root" "379845107" "379845108" "IP4" (get-in *config* [:common :local-address]))
                            (SessionNameField. "call")
                            (ConnectionField. "IP4" (get-in *config* [:common :local-address]))
                            (TimeField.))
    (.addMedia (MediaField. "audio" (int 50404) (int 0) "RTP/AVP" "8 0 101")
               (AttributeField. "rtpmap" "8 PCMA/8000"))))

(defn await!
  "Waits to receive a new object in the given core.async-channel
  within the given time in milliseconds. Returns the new object or
  ::timeout if the given time is exceeded."
  [chan timeout-ms]
  (let [timeout-chan (async/timeout timeout-ms)
        [val port] (async/alts!! [chan timeout-chan])]
    (if (= port timeout-chan) ::timeout val)))

(defn treat-received-event
  "Checks whether the given event is one of the expected ones and
  throws ExceptionInfo with some debug information if the event is
  unexpected. Returns nil."
  [event expected-event-names]
  (when-not (contains? (set expected-event-names) (::event/name event))
    (throw (ex-info (str "Got "
                         (::event/name event)
                         " instead of one of the expected ones")
                    {::expected expected-event-names
                     ::call-id (-> event
                                   ::mjsip/call
                                   .getInviteDialog
                                   .getCallID)}))))

(defn expect-events!
  "Pulls the received call events from the core.async-channel and
  assures that every event given in the event-names collection was
  received once in any order. Returns nil."
  [events-chan event-names]
  (loop [event-names event-names]
    (when-not (empty? event-names)
      (let [event (await! events-chan (get-in *config* [:common :default-timeout-ms]))]
        (when (= ::timeout event)
          ::timeout (throw (ex-info (str "Timeout while waiting for one of the expected events")
                                    {::expected event-names})))
        (treat-received-event event event-names)
        (recur (remove (-> event ::event/name list set) event-names))))))

(defn- ->callee-url [callee caller]
  (let [client-sip-url (callee->client-sip-url callee)]
    (if (extern-client? caller)
      (SipURL. (str (get-in *config* [:common :base-number])
                    (::client/call-number callee))
               (.getHost client-sip-url)
               (.getPort client-sip-url))
      client-sip-url)))

(defn invite
  "Initiates a new call. Returns a new outgoing call which can be used
  by await-call! to receive the incoming call."
  [caller callee]
  (let [call-events (async/chan (get-in *config* [:common :event-channel-buffer-size]))
        call (extended-call caller
                            {:default (fn [event]
                                        (async/>!! call-events event))})
        callee-url (->callee-url callee caller)]
    (.call (::mjsip/call call) (str callee-url))
    (assoc call ::call/events call-events)))

(defn transfer
  "Sends a REFER-Request to transfer the given incoming call to
  the given SipURL-object as target. Returns nil."
  [incoming-call target-url]
  (.transfer (::mjsip/call incoming-call) (str target-url)))

(defn transfer!
  "Transfers the given accepted incoming call to a new callee. The
  transferor should be the current callee of the given incoming
  call. Returns a new incoming call which can be used by
  accept-transfer! to accept it or refuse-transfer! to refuse it.
  wait-for-transferor! should be invoked with the given incoming call
  after the target conversation to assure it has been closed."
  [incoming-call transferor target-callee]
  (->> (->callee-url target-callee transferor)
       (transfer incoming-call))
  (expect-events! (::call/events incoming-call) [::event/call-transfer-accepted
                                                 ::event/call-transfer-success])
  (await-call! target-callee incoming-call))

(defn ring
  "Sends a Ringing-Response associated to the given call."
  [incoming-call]
  (.ring (::mjsip/call incoming-call) nil))

(defn ring!
  "Signals a ringing by the given unaccepted incoming call and assures
  that the peer-call has received it."
  [incoming-call peer-call]
  (ring incoming-call)
  (expect-events! (::call/events peer-call) [::event/call-ringing]))

(defn ack
  "Sends a ACK-Request associated with the given call."
  [call]
  (.ackWithAnswer (::mjsip/call call) nil))

(defn cancel
  "Sends a CANCEL-Request associeted with the given outgoing call."
  [call]
  (.cancel (::mjsip/call call)))

(defn cancel!
  "Cancels the given unaccepted call and assures that the peer call
  has received the denial."
  [call peer-call]
  (cancel call)
  (expect-events! (::call/events peer-call) [::event/call-cancelling])
  (expect-events! (::call/events call) [::event/call-refused]))

(defn busy
  "Sends a BUSY-Response associated with the given incoming call."
  [incoming-call]
  (.busy (::mjsip/call incoming-call)))

(defn busy!
  "Signals that the callee is busy by the given unaccepted incoming
  call and assures that the peer-call has received the denial."
  [incoming-call peer-call]
  (busy incoming-call)
  (expect-events! (::call/events peer-call) [::event/call-refused]))

(defn hangup
  "Sends a BYE-Request associeted with the given incoming call."
  [call]
  (.hangup (::mjsip/call call)))

(defn hangup!
  "Ends the given accepted call and assures that the peer-call has
  received the handup."
  [call peer-call]
  (hangup call)
  (expect-events! (::call/events peer-call) [::event/call-closing])
  (expect-events! (::call/events call) [::event/call-closed]))

(defn wait-for-transferor!
  "Waits for the event indicating that the given call has been
  transfered."
  [call]
  (expect-events! (::call/events call) [::event/call-closing]))

(defn accept
  "Sends a OK-Response to accept the open INVITE-Request."
  [incoming-call]
  (.accept (::mjsip/call incoming-call)
           (str (minimal-sdp))))

(defn accept!
  "Accepts the given unaccepted incoming call and assures that the
  peer-call has received the confirmation."
  [incoming-call peer-call]
  (accept incoming-call)
  (expect-events! (::call/events peer-call) [::event/call-accepted])
  (expect-events! (::call/events incoming-call) [::event/call-confirmed])
  (ack peer-call))

(defn accept-transfer!
  "Accepts the given unaccepted incoming call created by transfer!."
  [incoming-call]
  (accept incoming-call)
  (expect-events! (::call/events incoming-call) [::event/call-confirmed]))

(defn refuse-transfer!
  "Refuses the given unaccepted incoming call created by transfer! and
  assures that the peer-call has received the denial."
  [incoming-call peer-call]
  (hangup incoming-call)
  (expect-events! (::call/events peer-call) [::event/call-closing]))

(defn redirect
  "Sends a new INVITE-Request to the given SipURL-object representing
  the new callee. This redirects the unaccepted incoming call to a new
  callee."
  [incoming-call callee-url]
  (.redirect (::mjsip/call incoming-call) (str callee-url)))

(defn hold
  "Sends a ReINVITE-Request to set the given call to hold."
  [call]
  (let [sdp (.addAttribute (minimal-sdp) (AttributeField. "sendonly"))]
    (.modify (::mjsip/call call) nil (str sdp))))

(defn hold!
  "Holds the given accepted call. This call can be resumed later with
  resume!."
  [call]
  (hold call)
  (expect-events! (::call/events call) [::event/call-reinvite-accepted]))

(defn resume!
  "Resumes the given accepted call previously holded."
  [call]
  (let [sdp (.getRemoteSessionDescriptor (::mjsip/call call))]
    (.modify (::mjsip/call call) nil sdp)
    (expect-events! (::call/events call) [::event/call-reinvite-accepted])))

(defn register!
  "Registers the given SIP-client by its associated registrar."
  [client]
  (register client)
  (expect-events! (::registration/events client) [::event/registration-success]))

(defn await-call!
  "Waits for an invitation. reference-call is just for debugging
  purpose to identify the call referenced by this action in case of
  failure. Usually it is the outgoing call created by invite. Returns
  a new incoming call which further can be used by ring!, accept! or
  busy!."
  [callee reference-call]
  (let [call-id (-> reference-call ::mjsip/call .getInviteDialog .getCallID)
        calls-chan (::client/incoming-calls callee)
        incoming-call (await! calls-chan (get-in *config* [:common :default-timeout-ms]))]
    (when (= incoming-call ::timeout)
      (throw (ex-info "Didn't receive incoming call" {::call-id call-id})))
    incoming-call))

(defn- call->replaces-dialog-value [outgoing-call]
  (let [invite-dialog (.getInviteDialog (::mjsip/call outgoing-call))]
    (-> (str (.getCallID invite-dialog)
             ";to-tag=" (.getRemoteTag invite-dialog)
             ";from-tag=" (.getLocalTag invite-dialog))
        (URLEncoder/encode "UTF-8"))))

(defn replacing-transfer!
  "Initiates an attended transfer by replacing an accepted incoming
  call by another conversation giving the outgoing call as the
  target. The transferor should be the callee of the given accepted
  incoming call who acts as the transferor. The target-callee should
  be the callee of the targeting outgoing call. The given incoming
  call can be used by accept-transfer! to accept it or
  refuse-transfer! to refuse it. wait-for-transferor! should be
  invoked with the given incoming call after the target conversation
  to assure it has been closed."
  [incoming-call target-outgoing-call transferor target-callee]
  (->> (doto (->callee-url target-callee transferor)
         (.addParameter "Replaces" (call->replaces-dialog-value target-outgoing-call)))
       (transfer incoming-call))
  (expect-events! (::call/events incoming-call) [::event/call-transfer-accepted
                                                 ::event/call-transfer-success])
  (expect-events! (::call/events target-outgoing-call) [::event/call-closing]))

(defn redirect!
  "Redirects the given unaccepted incoming call to another
  callee. Returns a new incoming call with target-callee as the
  callee. This incoming call can further be used with accept!, busy!
  or ring!."
  [incoming-call target-callee]
  (redirect incoming-call (callee->client-sip-url target-callee))
  (await-call! target-callee incoming-call))

(defn halt!
  "Halts the given SIP-client which stops the listening for further
  events."
  [client]
  (.halt (::mjsip/sip-provider client))
  (.halt (::mjsip/register-agent client))
  (async/close! (::registration/events client)))
