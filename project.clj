(defproject teletrofono "0.1.0-SNAPSHOT"
  :description "A library written in clojure to write scenarios
testing the the Session Initiation Protocol (SIP) of a Back-to-Back
User Agent
(B2BUA)."
  :url "https://github.com/bevuta/teletrofono"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/core.async "0.4.474"]
                 [org.clojure/tools.logging "0.4.1"]
                 [log4j/log4j "1.2.17"]
                 [com.bevuta/mjsip-fork "1.6+lumicall.4"]
                 [org.opentelecoms.util/util "1.0.0"]])
