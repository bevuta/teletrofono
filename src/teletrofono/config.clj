(ns teletrofono.config
  (:require [clojure.spec.alpha :as s]))

(defn load-config [spec file]
  (let [config (load-file file)]
    (if (s/valid? spec config)
      config
      (throw (ex-info "Invalid configuration" (s/explain-data spec config))))))

(def ^:dynamic *config* nil)
