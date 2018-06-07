(ns teletrofono.performance-test
  (:require [clojure.core.async :as async]
            [clojure.test :refer :all]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]

            [teletrofono.core :as core]
            [teletrofono.config :refer [*config*
                                  load-config]]
            [teletrofono.utils :as utils]
            [teletrofono.test-utils :refer :all]
            [teletrofono.test-scenarios :refer :all]
            [teletrofono.common.config :as common.config]
            [teletrofono.performance-test.config :as config]))

(utils/child-ns client teletrofono.core.client)

(def ^:dynamic *clients*)

(defn performance-fixture [test-fn]
  (let [config (load-config (s/keys :req-un [::common.config/common ::config/performance-test])
                            "config.clj")
        common-client #::client{:realm (get-in config [:common :realm])
                                :local-address (get-in config [:common :local-address])
                                :registrar-address (get-in config [:common :registrar-address])}
        config-client-fn (get-in config [:performance-test :client-fn])]
    (binding [*config* config
              *clients* (->> (range (get-in config [:performance-test :clients]))
                             (map (fn [i]
                                    (-> (config-client-fn i)
                                        (qualify-keys (namespace ::client/_))
                                        (merge common-client)
                                        (core/client)))))]
      (->> *clients*
           (mapv (fn [client]
                   (Thread/sleep (get-in config [:performance-test :register-delay-ms]))
                   (async/thread (core/register! client) ::done)))
           (mapv async/<!!))
      (try (test-fn)
           (finally (doseq [client *clients*] (core/halt! client)))))))

(use-fixtures :once performance-fixture)

(defn run-scenario-catching
  "Runs the scenario function of a specific variation with a
  collection of SIP-clients catching any exception thrown in the
  scenario function and returning the exception-object. Investigate
  the scenario function to see which variations and how much clients
  it accepts."
  [scenario-fn variation clients]
  (try (do (apply scenario-fn variation clients) true)
       (catch Throwable e e)))

(defn timeout-pull!!
  "Pulls from the given core.async-channel blocking this thread within
  a maximum time. Returns ::timeout when the time exceeds or the
  pulled item otherwise."
  [chan]
  (let [timeout-chan (async/timeout (get-in *config* [:performance-test :thread-timeout-ms]))
        [item port] (async/alts!! [chan timeout-chan])]
    (if (= port timeout-chan) ::timeout item)))

(defn run-scenarios-longterm
  "Simulates longterm activity between the given SIP-clients. Runs
  the scenarios of the given collection in parallel and repeatedly for
  the given duration in minutes. The collection will be shuffled
  before every iteration, so the scenarios are picked randomly
  assuring every scenario has been chosen once after every
  iteration. Waits after every scenario between min-delay-s and
  max-delay-s seconds. threads specifies the maximum number of
  scenarios running in parallel. scenario-coll should be a collection of
  vectors with the scenario function as the first element, the
  variation as the second element and the count of clients the
  scenario is designed for as the third element."
  [scenario-coll
   min-delay-s max-delay-s
   duration-m
   threads
   clients]
  (let [;; Have to use a buffer size one less then the given count of threads
        ;; because this channel gets the result of the thread, so as soon as
        ;; an item is put the next thread has already been started.
        thread-chan (async/chan (dec threads))
        thread-result-seq (-> #(when-let [thread-result (async/<!! thread-chan)]
                                 (let [thread-chan (:thread-chan thread-result)]
                                   (-> thread-result
                                       (assoc :result (timeout-pull!! thread-chan))
                                       (dissoc :thread-chan))))
                              repeatedly)]
    (async/thread
      (let [client-chan (async/chan (count clients))
            end-time (+ (current-time) (* 60000 duration-m))
            start-thread (fn [scenario]
                           (let [[scenario-fn variation n-clients] scenario
                                 clients (->> #(async/<!! client-chan)
                                              (repeatedly)
                                              (take n-clients))
                                 thread-chan (async/thread
                                               (let [result (run-scenario-catching scenario-fn
                                                                                   variation
                                                                                   clients)]
                                                 (doseq [client clients]
                                                   (async/>!! client-chan client))
                                                 result))]
                             {:clients clients
                              :scenario-fn scenario-fn
                              :variation variation
                              :thread-chan thread-chan}))]
        ;; Fill the channel buffer with clients
        (doseq [client clients] (async/>!! client-chan client))
        ;; Loop for the duration of time
        (loop [scenario-seq (shuffle scenario-coll)]
          ;; Wait a little bit before we start the next thread
          (Thread/sleep (* 1000 (+ min-delay-s
                                   (rand-int (inc (- max-delay-s
                                                     min-delay-s))))))
          (async/>!! thread-chan (start-thread (first scenario-seq)))
          (if (< (current-time) end-time)
            (recur (if-let [scenario-seq (next scenario-seq)]
                     scenario-seq
                     (shuffle scenario-coll)))
            (async/close! thread-chan)))
        (async/close! client-chan)))
    (loop [thread-result-seq thread-result-seq
           n 1]
      (when-let [thread-result (first thread-result-seq)]
        (testing (str "Clients: " (str/join "," (map ::client/extension
                                                     (:clients thread-result))) "\n"
                      "Scenario: " (:scenario-fn thread-result) "\n"
                      "Variation: " (:variation thread-result) "\n"
                      "Thread: " n)
          (is (true? (:result thread-result))))
        (recur (next thread-result-seq) (inc n))))))

(deftest performance-test-longterm
  (let [scenario-coll [[call-without-conversation :a 2]
                       [call-without-conversation :b 2]
                       [call-with-conversation :a 2]
                       [call-with-conversation :b 2]
                       [call-with-conversation :long 2]
                       [call-hold-resume :a 2]
                       [call-hold-resume :b 2]
                       [consultation-call :a 3]
                       [consultation-call :b 3]
                       [attended-transfer-with-announcement :a 3]
                       [attended-transfer-with-announcement :b 3]
                       [attended-transfer-with-announcement :c 3]
                       [attended-transfer-without-announcement :a 3]
                       [attended-transfer-without-announcement :b 3]
                       [attended-transfer-without-announcement :c 3]
                       [unattended-transfer-without-announcement :a 3]
                       [unattended-transfer-without-announcement :b 3]
                       [unattended-transfer-without-announcement :c 3]
                       [direct-transfer :a 3]
                       [direct-transfer :b 3]
                       [direct-transfer :c 3]]
        {:keys [scenario-min-delay-s
                scenario-max-delay-s
                duration-m
                max-threads]} (:performance-test *config*)]
    (run-scenarios-longterm scenario-coll
                            scenario-min-delay-s
                            scenario-max-delay-s
                            duration-m
                            max-threads
                            *clients*)))
