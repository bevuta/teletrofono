{:common {;; Registration realm
          :realm "asterisk"

          ;; Network address of the local SIP-client
          :local-address "192.168.178.10"

          ;; Network address of the B2BUA the internal SIP-clients
          ;; will be registered on
          :registrar-address "10.10.10.10"

          ;; Buffer size of the event channels.
          :event-channel-buffer-size 200

          ;; Registration timeout in seconds. If this time exceeds
          ;; after the last resubscription, the SIP-client will be
          ;; treated as unavailable by the B2BUA
          :register-ttl-s 120

          ;; Time in seconds the SIP-client will be resubscribed to
          ;; keep the SIP-client available by the B2BUA.
          :register-renew-s 60

          ;; Time in milliseconds to wait for the excpected event by
          ;; the expect-events! function. Assuming a default T1 of
          ;; 500ms we use the default of 64 * T1 according to the
          ;; RFC3261
          :default-timeout-ms 32000

          ;; Base number of the B2BUAs public calling number. Will be
          ;; prepended to the call-number of the external client used
          ;; as a callee called by an internal client. So the B2BUA
          ;; should has configured a public calling number including
          ;; the extension to route to this external client.
          :base-number "49611"}
 :unit-tests {;; Three internal and one external SIP-clients.
              :clients {:intern-a {:display-name "Thomas Watson"
                                   :local-port 5061
                                   :extension 1001
                                   :call-number "54321001"
                                   :user "watson"
                                   :password "thomaswatson"}
                        :intern-b {:display-name "Graham Bell"
                                   :local-port 5062
                                   :extension 1002
                                   :call-number "54321002"
                                   :user "bell"
                                   :password "grahambell"}
                        :intern-c {:display-name "Elisha Gray"
                                   :local-port 5063
                                   :extension 1003
                                   :call-number "54321003"
                                   :user "gray"
                                   :password "elishagray"}
                        :extern {:display-name "Stranger"
                                 :local-port 5060
                                 :call-number "022354321004"}}}
 :performance-test {;; A function which gets an index number and
                    ;; returns the configuration of an internal
                    ;; SIP-client asigned to this index
                    :client-fn (fn [i]
                                 (let [id (+ 9000 i)]
                                   {:display-name (str "Testuser " id)
                                    :local-port (+ 49152 id)
                                    :extension (str id)
                                    :call-number (format "54321%03d" id)
                                    :user (str id)
                                    :password "testuser"}))

                    ;; Delay in milliseconds between each registration
                    ;; of the SIP-client designed to not overwhelm the
                    ;; registrar
                    :register-delay-ms 100

                    ;; Timeout in milliseconds for a running scenario
                    :thread-timeout-ms 120000

                    ;; Count of clients to use for the performance test
                    :clients 400

                    ;; Maximum number of parallel running scenarios
                    :max-threads 50

                    ;; Duration of the performance test
                    :duration-m 1

                    ;; Lower bound of the delay in seconds between
                    ;; every scenario
                    :scenario-delay-s 4

                    ;; Upper bound of the delay time in seconds
                    ;; between every scenario
                    :scenario-max-extra-delay-s 6}}
