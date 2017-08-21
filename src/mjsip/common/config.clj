(ns mjsip.common.config
  (:require [clojure.spec :as s]))

(s/def ::realm string?)
(s/def ::local-address string?)
(s/def ::registrar-address string?)
(s/def ::buffer-size pos?)
(s/def ::register-ttl-s pos?)
(s/def ::register-renew-s pos?)
(s/def ::default-timeout-ms pos?)
(s/def ::base-number string?)
(s/def ::common (s/keys :req-un [::realm
                                 ::local-address
                                 ::registrar-address
                                 ::buffer-size
                                 ::register-ttl-s
                                 ::register-renew-s
                                 ::default-timeout-ms
                                 ::base-number]))
