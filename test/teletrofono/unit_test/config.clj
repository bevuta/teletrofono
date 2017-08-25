(ns teletrofono.unit-test.config
  (:require [clojure.spec :as s]))

(s/def ::display-name string?)
(s/def ::local-port pos?)
(s/def ::extension pos?)
(s/def ::call-number string?)
(s/def ::user string?)
(s/def ::password string?)
(s/def ::intern-client (s/keys :req-un [::display-name
                                        ::local-port
                                        ::extension
                                        ::call-number
                                        ::user
                                        ::password]))
(s/def ::extern-client (s/keys :req-un [::display-name
                                        ::local-port
                                        ::call-number]))
(s/def ::intern-a ::intern-client)
(s/def ::intern-b ::intern-client)
(s/def ::intern-c ::intern-client)
(s/def ::extern ::extern-client)
(s/def ::clients (s/keys :req-un [::intern-a ::intern-b ::intern-c ::extern]))
(s/def ::unit-tests (s/keys :req-un [::clients]))
