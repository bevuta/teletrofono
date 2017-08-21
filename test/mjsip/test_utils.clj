(ns mjsip.test-utils
  (:require [clojure.test :refer :all]
            [clojure.java.shell :refer :all]
            [clojure.string :refer [split-lines
                                    split
                                    starts-with?]]
            [mjsip.config :refer [*config*]]
            [mjsip.utils :refer [child-ns]]))

(child-ns client mjsip.core.client)

(defn qualify-keys [m ns]
  (into {}
        (map (fn [[k v]]
               [(keyword ns (name k)) v]))
        m))

(def current-time #(System/currentTimeMillis))
