(ns teletrofono.unit-test.config
  (:require [clojure.spec.alpha :as s]))

(s/def ::display-name string?)
(s/def ::local-port pos?)
(s/def ::extension pos?)
(s/def ::user string?)
(s/def ::password string?)
(s/def ::client (s/keys :req-un [::display-name
                                 ::local-port
                                 ::extension
                                 ::user
                                 ::password]))
(s/def ::clients (s/coll-of ::client :kind vector? :count 3 :distinct true))
(s/def ::unit-tests (s/keys :req-un [::clients]))
