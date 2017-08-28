(ns teletrofono.common.config
  (:require [clojure.spec :as s]))

(s/def ::realm string?)
(s/def ::local-address string?)
(s/def ::registrar-address string?)
(s/def ::event-channel-buffer-size pos?)
(s/def ::register-ttl-s pos?)
(s/def ::register-renew-s pos?)
(s/def ::default-timeout-ms pos?)
(s/def ::common (s/keys :req-un [::realm
                                 ::local-address
                                 ::registrar-address
                                 ::event-channel-buffer-size
                                 ::register-ttl-s
                                 ::register-renew-s
                                 ::default-timeout-ms]))
