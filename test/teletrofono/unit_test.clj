(ns teletrofono.unit-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]

            [teletrofono.core :as core]
            [teletrofono.common.config :as common.config]
            [teletrofono.config :refer [*config*
                                  load-config]]
            [teletrofono.utils :as utils]
            [teletrofono.test-scenarios :refer :all]
            [teletrofono.test-utils :refer :all]
            [teletrofono.unit-test.config :as config]))

(utils/child-ns client teletrofono.core.client)

(def ^:dynamic *client-a*)
(def ^:dynamic *client-b*)
(def ^:dynamic *client-c*)

(defn unit-fixture [test-fn]
  (let [config (load-config (s/keys :req-un [::common.config/common ::config/unit-tests])
                            "config.clj")
        common-client #::client{:realm (get-in config [:common :realm])
                                :local-address (get-in config [:common :local-address])
                                :registrar-address (get-in config [:common :registrar-address])}
        clients (->> (get-in config [:unit-tests :clients])
                     (mapv (fn [client]
                             (-> client
                                 (qualify-keys (namespace ::client/_))
                                 (merge common-client)
                                 (core/client)))))]
    (binding [*config* config
              *client-a* (nth clients 0)
              *client-b* (nth clients 1)
              *client-c* (nth clients 2)]
      (doseq [client clients] (core/register! client))
      (try (test-fn)
           (finally
             (doseq [client clients] (core/halt! client)))))))

(use-fixtures :once unit-fixture)

(defn run-scenario [scenario-fn variation & clients]
  (is (nil? (apply scenario-fn variation clients))))

(deftest call-without-conversation_a
  (run-scenario call-without-conversation :a *client-a* *client-b*))

(deftest call-without-conversation_b
  (run-scenario call-without-conversation :b *client-a* *client-b*))

(deftest call-with-conversation_a
  (run-scenario call-with-conversation :a *client-a* *client-b*))

(deftest call-with-conversation_b
  (run-scenario call-with-conversation :b *client-a* *client-b*))

(deftest call-hold-resume_a
  (run-scenario call-hold-resume :a *client-a* *client-b*))

(deftest call-hold-resume_b
  (run-scenario call-hold-resume :b *client-a* *client-b*))

(deftest consultation-call_a
  (run-scenario consultation-call :a *client-a* *client-b* *client-c*))

(deftest consultation-call_b
  (run-scenario consultation-call :b *client-a* *client-b* *client-c*))

(deftest attended-transfer-with-announcement_a
  (run-scenario attended-transfer-with-announcement :a *client-a* *client-b* *client-c*))

(deftest attended-transfer-with-announcement_b
  (run-scenario attended-transfer-with-announcement :b *client-a* *client-b* *client-c*))

(deftest attended-transfer-with-announcement_c
  (run-scenario attended-transfer-with-announcement :c *client-a* *client-b* *client-c*))

(deftest attended-transfer-without-announcement_a
  (run-scenario attended-transfer-without-announcement :a *client-a* *client-b* *client-c*))

(deftest attended-transfer-without-announcement_b
  (run-scenario attended-transfer-without-announcement :b *client-a* *client-b* *client-c*))

(deftest attended-transfer-without-announcement_c
  (run-scenario attended-transfer-without-announcement :c *client-a* *client-b* *client-c*))

(deftest unattended-transfer-without-announcement_a
  (run-scenario unattended-transfer-without-announcement :a *client-a* *client-b* *client-c*))

(deftest unattended-transfer-without-announcement_b
  (run-scenario unattended-transfer-without-announcement :b *client-a* *client-b* *client-c*))

(deftest unattended-transfer-without-announcement_c
  (run-scenario unattended-transfer-without-announcement :c *client-a* *client-b* *client-c*))

(deftest direct-transfer_a
  (run-scenario direct-transfer :a *client-a* *client-b* *client-c*))

(deftest direct-transfer_b
  (run-scenario direct-transfer :b *client-a* *client-b* *client-c*))

(deftest direct-transfer_c
  (run-scenario direct-transfer :c *client-a* *client-b* *client-c*))
