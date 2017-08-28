{:common {;; Registration realm
          :realm "asterisk"

          ;; Network address of the local SIP-client
          :local-address "192.168.178.10"

          ;; Network address of the B2BUA the SIP-clients will be
          ;; registered on
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
          :default-timeout-ms 32000}
 :unit-tests {;; Three SIP-clients.
              :clients {[{:display-name "Thomas Watson"
                          :local-port 5061
                          :extension 1001
                          :user "watson"
                          :password "thomaswatson"}
                         {:display-name "Graham Bell"
                          :local-port 5062
                          :extension 1002
                          :user "bell"
                          :password "grahambell"}
                         {:display-name "Elisha Gray"
                          :local-port 5063
                          :extension 1003
                          :user "gray"
                          :password "elishagray"}]}}
 :performance-test {;; A function which gets an index number and
                    ;; returns the configuration of a SIP-client
                    ;; asigned to this index
                    :client-fn (fn [i]
                                 (let [id (+ 1000 i)]
                                   {:display-name (str "Testuser " id)
                                    :local-port (+ 49152 id)
                                    :extension (str id)
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
                    :duration-m 5

                    ;; Minimum delay in seconds after every scenario
                    :scenario-min-delay-s 4

                    ;; Maximum delay in seconds after every scenario
                    :scenario-max-delay-s 10}}
