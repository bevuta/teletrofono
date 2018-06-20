{;; The common section contains all options not directly constrained
 ;; to a testing section but may used for common purposes by the
 ;; testing sections below.
 :common {;; Registration realm.
          :realm "asterisk"

          ;; Network address of the local SIP-client.
          :local-address "192.168.178.10"

          ;; Network address of the B2BUA containing the appropriate
          ;; SIP-accounts for the SIP-clients configured in this file.
          :registrar-address "10.10.10.10"

          ;; Registration timeout in seconds. If this time exceeds
          ;; after the last resubscription, the SIP-client will be
          ;; treated as unavailable by the B2BUA.
          :register-ttl-s 120

          ;; Time in seconds the SIP-client will be resubscribed to
          ;; keep the SIP-client available by the B2BUA.
          :register-renew-s 60

          ;; Time in milliseconds to wait for the excpected event by
          ;; the expect-events! function. Assuming a default T1 of
          ;; 500ms we use the default of 64 * T1 according to the
          ;; RFC3261.
          :default-timeout-ms 32000

          ;; Buffer size of the event channels.
          :event-channel-buffer-size 200}

 ;; This testing section contains the options required by the unit
 ;; tests.
 :unit-tests {;; Three SIP-clients. There are exactly three because
              ;; the predefined scenario functions require a maximum
              ;; of three clients. Each of these clients should have
              ;; an own registered SIP-account on the B2BUA.
              :clients [{;; The display name of the SIP-client which
                         ;; appears as a text representation of the
                         ;; SIP-address.
                         :display-name "Thomas Watson"

                         ;; The local port of the SIP-client. Usually
                         ;; its 5060 or 5061 but here we use multiple
                         ;; SIP-clients on the same machine, so we
                         ;; use the dynamic/private port
                         ;; range (49152-65535). This range can be
                         ;; used by any application allocated
                         ;; randomly. However the ports are fixed
                         ;; here and aren't allocated dynamicly, so
                         ;; there is a chance that these ports are
                         ;; used by already running applications.
                         :local-port 49152

                         ;; The extension to the call number
                         ;; registered on the B2BUA for this
                         ;; SIP-account.
                         :extension 1001

                         ;; The authentication username.
                         :user "watson"

                         ;; The authentication password.
                         :password "thomaswatson"}

                        {:display-name "Graham Bell"
                         :local-port 49153
                         :extension 1002
                         :user "bell"
                         :password "grahambell"}
                        {:display-name "Elisha Gray"
                         :local-port 49154
                         :extension 1003
                         :user "gray"
                         :password "elishagray"}]}
 ;; This testing section contains the options required by the
 ;; performance test. The performance tests in contrast to the unit
 ;; tests have a configurable amount of clients.
 :performance-test {;; A function which gets an index number and
                    ;; returns the configuration of a SIP-client
                    ;; assigned to this index.
                    :client-fn (fn [i]
                                 (let [id (+ 1000 i)]
                                   {:display-name (str "Testuser " id)
                                    :local-port (+ 49152 id)
                                    :extension (str id)
                                    :user (str id)
                                    :password "testuser"}))

                    ;; Delay in milliseconds between each registration
                    ;; of the SIP-client designed to not overwhelm the
                    ;; registrar.
                    :register-delay-ms 100

                    ;; Timeout in milliseconds for a running scenario.
                    :thread-timeout-ms 120000

                    ;; Count of clients to use for the performance
                    ;; test.  It should be at least four clients. One
                    ;; more then the number of clients a scenario with
                    ;; the most attendants requires
                    :clients 400

                    ;; Maximum number of parallel running scenarios.
                    :max-threads 50

                    ;; Duration of the performance test.
                    :duration-m 5

                    ;; Minimum delay in seconds after every scenario.
                    :scenario-min-delay-s 4

                    ;; Maximum delay in seconds after every scenario.
                    :scenario-max-delay-s 10}}
