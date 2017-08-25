(ns teletrofono.test-utils
  (:require [clojure.test :refer :all]
            [clojure.java.shell :refer :all]
            [clojure.string :refer [split-lines
                                    split
                                    starts-with?]]
            [teletrofono.config :refer [*config*]]
            [teletrofono.utils :refer [child-ns]]))

(child-ns client teletrofono.core.client)

(defn qualify-keys [m ns]
  (into {}
        (map (fn [[k v]]
               [(keyword ns (name k)) v]))
        m))

(def current-time #(System/currentTimeMillis))
