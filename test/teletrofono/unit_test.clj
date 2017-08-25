(ns teletrofono.unit-test
  (:require [clojure.test :refer :all]
            [clojure.spec :as s]

            [teletrofono.core :as core]
            [teletrofono.common.config :as common.config]
            [teletrofono.config :refer [*config*
                                  load-config]]
            [teletrofono.utils :as utils]
            [teletrofono.test-scenarios :refer :all]
            [teletrofono.test-utils :refer :all]
            [teletrofono.unit-test.config :as config]))

(utils/child-ns client teletrofono.core.client)

(def ^:dynamic *intern-a*)
(def ^:dynamic *intern-b*)
(def ^:dynamic *intern-c*)
(def ^:dynamic *extern*)

(defn unit-fixture [test-fn]
  (let [config (load-config (s/keys :req-un [::common.config/common ::config/unit-tests])
                            "config.clj")
        common-client #::client{:realm (get-in config [:common :realm])
                                :local-address (get-in config [:common :local-address])
                                :registrar-address (get-in config [:common :registrar-address])}
        clients (get-in config [:unit-tests :clients])
        get-client (fn [k t]
                     (-> (get clients k)
                         (qualify-keys (namespace ::client/_))
                         (merge common-client {::client/type t})
                         (core/client)))]
    (binding [*config* config
              *intern-a* (get-client :intern-a :intern)
              *intern-b* (get-client :intern-b :intern)
              *intern-c* (get-client :intern-c :intern)
              *extern* (get-client :extern :extern)]
      (core/register! *intern-a*)
      (core/register! *intern-b*)
      (core/register! *intern-c*)
      (try (test-fn)
           (finally
             (core/halt! *extern*)
             (core/halt! *intern-a*)
             (core/halt! *intern-b*)
             (core/halt! *intern-c*))))))

(use-fixtures :once unit-fixture)

(defn run-scenario [scenario-fn variation & clients]
  (is (nil? (apply scenario-fn variation clients))))

(deftest call-without-conversation_a_ii
  (run-scenario call-without-conversation :a *intern-a* *intern-b*))

(deftest call-without-conversation_a_ie
  (run-scenario call-without-conversation :a *intern-a* *extern*))

(deftest call-without-conversation_a_ei
  (run-scenario call-without-conversation :a *extern* *intern-a*))

(deftest call-without-conversation_b_ii
  (run-scenario call-without-conversation :b *intern-a* *intern-b*))

(deftest call-without-conversation_b_ie
  (run-scenario call-without-conversation :b *intern-a* *extern*))

(deftest call-without-conversation_b_ei
  (run-scenario call-without-conversation :b *extern* *intern-a*))

(deftest call-with-conversation_a_ii
  (run-scenario call-with-conversation :a *intern-a* *intern-b*))

(deftest call-with-conversation_a_ie
  (run-scenario call-with-conversation :a *intern-a* *extern*))

(deftest call-with-conversation_a_ei
  (run-scenario call-with-conversation :a *extern* *intern-a*))

(deftest call-with-conversation_b_ii
  (run-scenario call-with-conversation :b *intern-a* *intern-b*))

(deftest call-with-conversation_b_ie
  (run-scenario call-with-conversation :b *intern-a* *extern*))

(deftest call-with-conversation_b_ei
  (run-scenario call-with-conversation :b *extern* *intern-a*))

(deftest call-hold-resume_a_ii
  (run-scenario call-hold-resume :a *intern-a* *intern-b*))

(deftest call-hold-resume_a_ie
  (run-scenario call-hold-resume :a *intern-a* *extern*))

(deftest call-hold-resume_a_ei
  (run-scenario call-hold-resume :a *extern* *intern-a*))

(deftest call-hold-resume_b_ii
  (run-scenario call-hold-resume :b *intern-a* *intern-b*))

(deftest call-hold-resume_b_ie
  (run-scenario call-hold-resume :b *intern-a* *extern*))

(deftest call-hold-resume_b_ei
  (run-scenario call-hold-resume :b *extern* *intern-a*))

(deftest consultation-call_a_iii
  (run-scenario consultation-call :a *intern-a* *intern-b* *intern-c*))

(deftest consultation-call_a_eii
  (run-scenario consultation-call :a *extern* *intern-a* *intern-b*))

(deftest consultation-call_a_iie
  (run-scenario consultation-call :a *intern-a* *intern-b* *extern*))

(deftest consultation-call_b_iii
  (run-scenario consultation-call :b *intern-a* *intern-b* *intern-c*))

(deftest consultation-call_b_eii
  (run-scenario consultation-call :b *extern* *intern-a* *intern-b*))

(deftest consultation-call_b_iie
  (run-scenario consultation-call :b *intern-a* *intern-b* *extern*))

(deftest attended-transfer-with-announcement_a_iii
  (run-scenario attended-transfer-with-announcement :a *intern-a* *intern-b* *intern-c*))

(deftest attended-transfer-with-announcement_a_eii
  (run-scenario attended-transfer-with-announcement :a *extern* *intern-a* *intern-b*))

(deftest attended-transfer-with-announcement_a_iie
  (run-scenario attended-transfer-with-announcement :a *intern-a* *intern-b* *extern*))

(deftest attended-transfer-with-announcement_b_iii
  (run-scenario attended-transfer-with-announcement :b *intern-a* *intern-b* *intern-c*))

(deftest attended-transfer-with-announcement_b_eii
  (run-scenario attended-transfer-with-announcement :b *extern* *intern-a* *intern-b*))

(deftest attended-transfer-with-announcement_b_iie
  (run-scenario attended-transfer-with-announcement :b *intern-a* *intern-b* *extern*))

(deftest attended-transfer-with-announcement_c_iii
  (run-scenario attended-transfer-with-announcement :c *intern-a* *intern-b* *intern-c*))

(deftest attended-transfer-with-announcement_c_eii
  (run-scenario attended-transfer-with-announcement :c *extern* *intern-a* *intern-b*))

(deftest attended-transfer-with-announcement_c_iie
  (run-scenario attended-transfer-with-announcement :c *intern-a* *intern-b* *extern*))

(deftest attended-transfer-without-announcement_a_iii
  (run-scenario attended-transfer-without-announcement :a *intern-a* *intern-b* *intern-c*))

(deftest attended-transfer-without-announcement_a_eii
  (run-scenario attended-transfer-without-announcement :a *extern* *intern-a* *intern-b*))

(deftest attended-transfer-without-announcement_a_iie
  (run-scenario attended-transfer-without-announcement :a *intern-a* *intern-b* *extern*))

(deftest attended-transfer-without-announcement_b_iii
  (run-scenario attended-transfer-without-announcement :b *intern-a* *intern-b* *intern-c*))

(deftest attended-transfer-without-announcement_b_eii
  (run-scenario attended-transfer-without-announcement :b *extern* *intern-a* *intern-b*))

(deftest attended-transfer-without-announcement_b_iie
  (run-scenario attended-transfer-without-announcement :b *intern-a* *intern-b* *extern*))

(deftest attended-transfer-without-announcement_c_iii
  (run-scenario attended-transfer-without-announcement :c *intern-a* *intern-b* *intern-c*))

(deftest attended-transfer-without-announcement_c_eii
  (run-scenario attended-transfer-without-announcement :c *extern* *intern-a* *intern-b*))

(deftest attended-transfer-without-announcement_c_iie
  (run-scenario attended-transfer-without-announcement :c *intern-a* *intern-b* *extern*))

(deftest unattended-transfer-without-announcement_a_iii
  (run-scenario unattended-transfer-without-announcement :a *intern-a* *intern-b* *intern-c*))

(deftest unattended-transfer-without-announcement_a_eii
  (run-scenario unattended-transfer-without-announcement :a *extern* *intern-a* *intern-b*))

(deftest unattended-transfer-without-announcement_a_iie
  (run-scenario unattended-transfer-without-announcement :a *intern-a* *intern-b* *extern*))

(deftest unattended-transfer-without-announcement_b_iii
  (run-scenario unattended-transfer-without-announcement :b *intern-a* *intern-b* *intern-c*))

(deftest unattended-transfer-without-announcement_b_eii
  (run-scenario unattended-transfer-without-announcement :b *extern* *intern-a* *intern-b*))

(deftest unattended-transfer-without-announcement_b_iie
  (run-scenario unattended-transfer-without-announcement :b *intern-a* *intern-b* *extern*))

(deftest unattended-transfer-without-announcement_c_iii
  (run-scenario unattended-transfer-without-announcement :c *intern-a* *intern-b* *intern-c*))

(deftest unattended-transfer-without-announcement_c_eii
  (run-scenario unattended-transfer-without-announcement :c *extern* *intern-a* *intern-b*))

(deftest unattended-transfer-without-announcement_c_iie
  (run-scenario unattended-transfer-without-announcement :c *intern-a* *intern-b* *extern*))

(deftest direct-transfer_a_iii
  (run-scenario direct-transfer :a *intern-a* *intern-b* *intern-c*))

(deftest direct-transfer_a_eii
  (run-scenario direct-transfer :a *extern* *intern-a* *intern-b*))

(deftest direct-transfer_a_iie
  (run-scenario direct-transfer :a *intern-a* *intern-b* *extern*))

(deftest direct-transfer_b_iii
  (run-scenario direct-transfer :b *intern-a* *intern-b* *intern-c*))

(deftest direct-transfer_b_eii
  (run-scenario direct-transfer :b *extern* *intern-a* *intern-b*))

(deftest direct-transfer_b_iie
  (run-scenario direct-transfer :b *intern-a* *intern-b* *extern*))

(deftest direct-transfer_c_iii
  (run-scenario direct-transfer :c *intern-a* *intern-b* *intern-c*))

(deftest direct-transfer_c_eii
  (run-scenario direct-transfer :c *extern* *intern-a* *intern-b*))

(deftest direct-transfer_c_iie
  (run-scenario direct-transfer :c *intern-a* *intern-b* *extern*))
