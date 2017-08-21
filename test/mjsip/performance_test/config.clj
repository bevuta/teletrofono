(ns mjsip.performance-test.config
  (:require [clojure.spec :as s]))

(s/def ::client-fn fn?)
(s/def ::register-delay-ms nat-int?)
(s/def ::thread-timeout-ms pos?)
(s/def ::clients pos?)
(s/def ::max-threads pos?)
(s/def ::duration-m pos?)
(s/def ::scenario-delay-s nat-int?)
(s/def ::scenario-max-extra-delay-s nat-int?)
(s/def ::performance-test (s/keys :req-un [::client-fn
                                           ::register-delay-ms
                                           ::thread-timeout-ms
                                           ::clients
                                           ::duration-m
                                           ::scenario-delay-s
                                           ::scenario-max-extra-delay-s]))
