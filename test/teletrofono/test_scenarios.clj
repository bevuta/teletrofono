(ns teletrofono.test-scenarios
  (:require [teletrofono.core :refer :all]))

(def +wait-ms+ 2000)
(def +long-conversation-m+ 5)

(defn wait
  "Simulates a human delay."
  []
  (Thread/sleep +wait-ms+))

(defn long-conversation-wait
  "Simulates a long conversation."
  []
  (Thread/sleep (* +long-conversation-m+ 60000)))

(defn call-without-conversation
  "Simulates a call accepting it.
  Variations:
  :a - watson hangs up
  :b - bell hangs up "
  [variation watson bell]
  (let [outgoing-call (invite watson bell)
        incoming-call (await-call! bell outgoing-call)]
    (ring! incoming-call outgoing-call)
    (wait)
    (case variation
      :a (cancel! outgoing-call incoming-call)
      :b (busy! incoming-call outgoing-call))))

(defn call-with-conversation
  "Simulates a call with a conversation.
  Variations:
  :a - bell hangs up
  :b - watson hangs up
  :long - long conversation"
  [variation watson bell]
  (let [outgoing-call (invite watson bell)
        incoming-call (await-call! bell outgoing-call)]
    (ring! incoming-call outgoing-call)
    (wait)
    (accept! incoming-call outgoing-call)
    (if (= :long variation)
      (long-conversation-wait)
      (wait))
    (case variation
      :a (hangup! incoming-call outgoing-call)
      (hangup! outgoing-call incoming-call))))

(defn call-hold-resume
  "Simulates a call with a conversation holded and resumed once.
  Variations:
  :a - bell holds and resumes and does a hangup after that
  :b - watson holds and resumes and does a hangup after that"
  [variation watson bell]
  (let [outgoing-call (invite watson bell)
        incoming-call (await-call! bell outgoing-call)]
    (ring! incoming-call outgoing-call)
    (wait)
    (accept! incoming-call outgoing-call)
    (wait)
    (case variation
      :a (do (hold! incoming-call)
             (wait)
             (resume! incoming-call)
             (wait)
             (hangup! incoming-call outgoing-call))
      :b (do (hold! outgoing-call)
             (wait)
             (resume! outgoing-call)
             (wait)
             (hangup! outgoing-call incoming-call)))))

(defn consultation-call
  "Simulates a call between watson and bell with bell consulting gray
  during the call. Bell hangs up at the end.
  Variations:
  :a - gray hangs up the consultational call
  :b - bell hangs up the consultational call"
  [variation watson bell gray]
  (let [outgoing-call-watson (invite watson bell)
        incoming-call-bell (await-call! bell outgoing-call-watson)]
    (ring! incoming-call-bell outgoing-call-watson)
    (wait)
    (accept! incoming-call-bell outgoing-call-watson)
    (wait)
    (hold! incoming-call-bell)
    (let [outgoing-call-bell (invite bell gray)
          incoming-call-gray (await-call! gray outgoing-call-bell)]
      (ring! incoming-call-gray outgoing-call-bell)
      (wait)
      (accept! incoming-call-gray outgoing-call-bell)
      (wait)
      (case variation
        :a (hangup! incoming-call-gray outgoing-call-bell)
        :b (hangup! outgoing-call-bell incoming-call-gray)))
    (resume! incoming-call-bell)
    (wait)
    (hangup! incoming-call-bell outgoing-call-watson)))

(defn attended-transfer-with-announcement
  "Simulates a call between watson and bell with bell consulting gray
  during the call and initiating an attended transfer to establish a
  conversation between watson and gray.
  Variations:
  :a - gray hangs up the call with watson
  :b - watson hangs up the call with gray
  :c - gray signals business on the consultational call from bell"
  [variation watson bell gray]
  (let [outgoing-call-watson (invite watson bell)
        incoming-call-bell (await-call! bell outgoing-call-watson)]
    (ring! incoming-call-bell outgoing-call-watson)
    (wait)
    (accept! incoming-call-bell outgoing-call-watson)
    (wait)
    (hold! incoming-call-bell)
    (let [outgoing-call-bell (invite bell gray)
          incoming-call-gray (await-call! gray outgoing-call-bell)]
      (if-not (= variation :c)
        (do
          (ring! incoming-call-gray outgoing-call-bell)
          (wait)
          (accept! incoming-call-gray outgoing-call-bell)
          (wait)
          (replacing-transfer! incoming-call-bell outgoing-call-bell gray)
          (wait)
          (case variation
            :a (hangup! incoming-call-gray outgoing-call-watson)
            :b (hangup! outgoing-call-watson incoming-call-gray))
          (wait-for-transferor! incoming-call-bell))
        (do (busy! incoming-call-gray outgoing-call-bell)
            (wait)
            (hangup! incoming-call-bell outgoing-call-watson))))))

(defn attended-transfer-without-announcement
  "Simulates a call between watson and bell with bell attempting to
  call gray during the call without gray accepting and bell initiating an
  attended transfer to establish a conversation with watson and gray.
  Variations:
  :a - gray hangs up the call with watson
  :b - watson hangs up the call with gray
  :c - gray refuses the conversation with watson"
  [variation watson bell gray]
  (let [outgoing-call-watson (invite watson bell)
        incoming-call-bell (await-call! bell outgoing-call-watson)]
    (ring! incoming-call-bell outgoing-call-watson)
    (wait)
    (accept! incoming-call-bell outgoing-call-watson)
    (wait)
    (hold! incoming-call-bell)
    (let [outgoing-call-bell (invite bell gray)
          incoming-call-gray (await-call! gray outgoing-call-bell)]
      (ring! incoming-call-gray outgoing-call-bell)
      (wait)
      (cancel! outgoing-call-bell incoming-call-gray)
      (let [incoming-call-gray (transfer! incoming-call-bell gray)]
        (wait)
        (case variation
          :a (do (accept-transfer! incoming-call-gray)
                 (wait)
                 (hangup! incoming-call-gray outgoing-call-watson))
          :b (do (accept-transfer! incoming-call-gray)
                 (wait)
                 (hangup! outgoing-call-watson incoming-call-gray))
          :c (refuse-transfer! incoming-call-gray outgoing-call-watson))
        (wait-for-transferor! incoming-call-bell)))))

(defn unattended-transfer-without-announcement
  "Simulates a call between watson and bell with bell transfering the
  call directly to gray.
  Variations:
  :a - gray hangs up the call with watson
  :b - watson hangs up the call with gray
  :c - gray refuses the conversation with watson"
  [variation watson bell gray]
  (let [outgoing-call-watson (invite watson bell)
        incoming-call-bell (await-call! bell outgoing-call-watson)]
    (ring! incoming-call-bell outgoing-call-watson)
    (wait)
    (accept! incoming-call-bell outgoing-call-watson)
    (wait)
    (hold! incoming-call-bell)
    (wait)
    (let [incoming-call-gray (transfer! incoming-call-bell gray)]
      (case variation
        :a (do (accept-transfer! incoming-call-gray)
               (wait)
               (hangup! incoming-call-gray outgoing-call-watson))
        :b (do (accept-transfer! incoming-call-gray)
               (wait)
               (hangup! outgoing-call-watson incoming-call-gray))
        :c (refuse-transfer! incoming-call-gray outgoing-call-watson))
      (wait-for-transferor! incoming-call-bell))))

(defn direct-transfer
  "Simulates a call between watson and bell with bell redirecting the
  call directly to gray without accepting it.
  Variations:
  :a - gray hangs up the call with watson
  :b - watson hangs up the call with gray
  :c - gray refuses the conversation with watson"
  [variation watson bell gray]
  (let [outgoing-call-watson (invite watson bell)
        incoming-call-bell (await-call! bell outgoing-call-watson)]
    (ring! incoming-call-bell outgoing-call-watson)
    (wait)
    (let [incoming-call-gray (redirect! incoming-call-bell gray)]
      ;; watson doesn't receive this Ringing-Response,
      ;; because the B2BUA doesn't forward it, therefore use just
      ;; the "ring"-fn instead of "ring!"
      (ring incoming-call-gray)
      (wait)
      (case variation
        :a (do (accept! incoming-call-gray outgoing-call-watson)
               (wait)
               (hangup! incoming-call-gray outgoing-call-watson))
        :b (do (accept! incoming-call-gray outgoing-call-watson)
               (wait)
               (hangup! outgoing-call-watson incoming-call-gray))
        :c (busy! incoming-call-gray outgoing-call-watson)))))
