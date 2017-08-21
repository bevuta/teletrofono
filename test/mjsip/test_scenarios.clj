(ns mjsip.test-scenarios
  (:require [mjsip.core :refer :all]))

(def +wait-ms+ 2000)
(def +long-conversation-m+ 5)

;; NOTE: The purpose of this fn is to simulate a human delay
(defn wait []
  (Thread/sleep +wait-ms+))

(defn long-conversation-wait []
  (Thread/sleep (* +long-conversation-m+ 60000)))

(defn call-without-conversation [variation watson bell]
  (let [outgoing-call (invite watson bell)
        incoming-call (await-call! bell outgoing-call)]
    (ring! incoming-call outgoing-call)
    (wait)
    (case variation
      :a (cancel! outgoing-call incoming-call)
      :b (busy! incoming-call outgoing-call))))

(defn call-with-conversation [variation watson bell]
  (let [outgoing-call (invite watson bell)
        incoming-call (await-call! bell outgoing-call)]
    (ring! incoming-call outgoing-call)
    (wait)
    (accept! incoming-call outgoing-call)
    (if (= :long variation)
      (long-conversation-wait)
      (wait))
    (case variation
      :a (hangup! incoming-call outgoing-call)
      (hangup! outgoing-call incoming-call))))

(defn call-hold-resume [variation watson bell]
  (let [outgoing-call (invite watson bell)
        incoming-call (await-call! bell outgoing-call)]
    (ring! incoming-call outgoing-call)
    (wait)
    (accept! incoming-call outgoing-call)
    (wait)
    (case variation
      :a (do (hold! incoming-call)
             (wait)
             (resume! incoming-call)
             (wait)
             (hangup! incoming-call outgoing-call))
      :b (do (hold! outgoing-call)
             (wait)
             (resume! outgoing-call)
             (wait)
             (hangup! outgoing-call incoming-call)))))

(defn consultation-call [variation watson bell gray]
  (let [outgoing-call-watson (invite watson bell)
        incoming-call-bell (await-call! bell outgoing-call-watson)]
    (ring! incoming-call-bell outgoing-call-watson)
    (wait)
    (accept! incoming-call-bell outgoing-call-watson)
    (wait)
    (hold! incoming-call-bell)
    (let [outgoing-call-bell (invite bell gray)
          incoming-call-gray (await-call! gray outgoing-call-bell)]
      (ring! incoming-call-gray outgoing-call-bell)
      (wait)
      (accept! incoming-call-gray outgoing-call-bell)
      (wait)
      (case variation
        :a (hangup! incoming-call-gray outgoing-call-bell)
        :b (hangup! outgoing-call-bell incoming-call-gray)))
    (resume! incoming-call-bell)
    (wait)
    (hangup! incoming-call-bell outgoing-call-watson)))

(defn attended-transfer-with-announcement [variation watson bell gray]
  (let [outgoing-call-watson (invite watson bell)
        incoming-call-bell (await-call! bell outgoing-call-watson)]
    (ring! incoming-call-bell outgoing-call-watson)
    (wait)
    (accept! incoming-call-bell outgoing-call-watson)
    (wait)
    (hold! incoming-call-bell)
    (let [outgoing-call-bell (invite bell gray)
          incoming-call-gray (await-call! gray outgoing-call-bell)]
      (if-not (= variation :c)
        (do
          (ring! incoming-call-gray outgoing-call-bell)
          (wait)
          (accept! incoming-call-gray outgoing-call-bell)
          (wait)
          (replacing-transfer! incoming-call-bell outgoing-call-bell bell gray)
          (wait)
          (case variation
            :a (hangup! incoming-call-gray outgoing-call-watson)
            :b (hangup! outgoing-call-watson incoming-call-gray))
          (wait-for-transferor! incoming-call-bell))
        (do (busy! incoming-call-gray outgoing-call-bell)
            (wait)
            (hangup! incoming-call-bell outgoing-call-watson))))))

(defn attended-transfer-without-announcement [variation watson bell gray]
  (let [outgoing-call-watson (invite watson bell)
        incoming-call-bell (await-call! bell outgoing-call-watson)]
    (ring! incoming-call-bell outgoing-call-watson)
    (wait)
    (accept! incoming-call-bell outgoing-call-watson)
    (wait)
    (hold! incoming-call-bell)
    (let [outgoing-call-bell (invite bell gray)
          incoming-call-gray (await-call! gray outgoing-call-bell)]
      (ring! incoming-call-gray outgoing-call-bell)
      (wait)
      (cancel! outgoing-call-bell incoming-call-gray)
      (let [incoming-call-gray (transfer! incoming-call-bell bell gray)]
        (wait)
        (case variation
          :a (do (accept-transfer! incoming-call-gray)
                 (wait)
                 (hangup! incoming-call-gray outgoing-call-watson))
          :b (do (accept-transfer! incoming-call-gray)
                 (wait)
                 (hangup! outgoing-call-watson incoming-call-gray))
          :c (refuse-transfer! incoming-call-gray outgoing-call-watson))
        (wait-for-transferor! incoming-call-bell)))))

(defn unattended-transfer-without-announcement [variation watson bell gray]
  (let [outgoing-call-watson (invite watson bell)
        incoming-call-bell (await-call! bell outgoing-call-watson)]
    (ring! incoming-call-bell outgoing-call-watson)
    (wait)
    (accept! incoming-call-bell outgoing-call-watson)
    (wait)
    (hold! incoming-call-bell)
    (wait)
    (let [incoming-call-gray (transfer! incoming-call-bell bell gray)]
      (case variation
        :a (do (accept-transfer! incoming-call-gray)
               (wait)
               (hangup! incoming-call-gray outgoing-call-watson))
        :b (do (accept-transfer! incoming-call-gray)
               (wait)
               (hangup! outgoing-call-watson incoming-call-gray))
        :c (refuse-transfer! incoming-call-gray outgoing-call-watson))
      (wait-for-transferor! incoming-call-bell))))

(defn direct-transfer [variation watson bell gray]
  (let [outgoing-call-watson (invite watson bell)
        incoming-call-bell (await-call! bell outgoing-call-watson)]
    (ring! incoming-call-bell outgoing-call-watson)
    (wait)
    (let [incoming-call-gray (redirect! incoming-call-bell gray)]
      ;; watson doesn't receive this Ringing-Response,
      ;; because the B2BUA doesn't forward it, therefore use just
      ;; the "ring"-fn instead of "ring!"
      (ring incoming-call-gray)
      (wait)
      (case variation
        :a (do (accept! incoming-call-gray outgoing-call-watson)
               (wait)
               (hangup! incoming-call-gray outgoing-call-watson))
        :b (do (accept! incoming-call-gray outgoing-call-watson)
               (wait)
               (hangup! outgoing-call-watson incoming-call-gray))
        :c (busy! incoming-call-gray outgoing-call-watson)))))
