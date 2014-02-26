(defproject grimoire "20140226-1"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.twitter4j/twitter4j-core"[3.0,)"]
                 [org.twitter4j/twitter4j-stream"[3.0,)"]
                 [org.twitter4j/twitter4j-async"[3.0,)"]
                 [enlive "1.1.4"]
                 [ring/ring-jetty-adapter "1.1.6"]
                 [org.clojure/tools.nrepl "0.2.3"]]
  :main grimoire.core
  :java-source-paths ["src/java"])
