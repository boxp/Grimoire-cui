(ns grimoire.core
  (:use [clojure.repl]
        [grimoire.oauth :as oauth]
        [grimoire.commands]
        [grimoire.services]
        [grimoire.listener]
        [grimoire.data]
        [grimoire.plugin])
  (:import (java.io File))
  (:require [ring.adapter.jetty :as jetty])
  (:gen-class))


; 起動時に呼ばれる
; dirty
(defn -main
  [& args]
  (do
    ; コンソール向け
    (get-oauthtoken!)
    (try 
      (do 
        (get-tokens)
        (gen-twitter)
        (gen-twitterstream (listener))
        (start))
      (catch Exception e 
        (do
          (println "Please get Pin code")
          (println (.getAuthorizationURL @oauthtoken))
          (print "PIN:")
          (flush)
          (gen-tokens (read-line))
          (get-tokens)
          (gen-twitter)
          (gen-twitterstream (listener))
          (start))))

    ; Heroku 向け jetty サーバ
    (if (empty? args)
      nil
      (let [app (fn [req] 
                  {:status 200
                   :headers {"Content-Type" "text/plain"}
                   :body "Hello, world"})]
        (jetty/run-jetty app {:port (Integer. (first args)) :join? false})))

    ; タイトル
    (reset! myname (. twitter getScreenName))
    (print
      "Grimoire has started v20131114-1\n"
      "_ ........_\n"
      ", ´,.-==-.ヽ\n"
      "l ((ﾉﾉ))ﾉ）)\n"
      "ハ) ﾟ ヮﾟﾉ)\n"
      "~,く__,ネﾉ)つ\n"
      "|(ﾝ_l|,_,_ﾊ、\n"
      "｀~ し'.ﾌ~´\n"

     (try
       (str "Welcome " @myname "!\n")
       (catch Exception e nil))
     "---------------------------\n"
     "* Grimoire user guide     *\n"
     "---------------------------\n"
     "* Emode : e[Enter]        *\n"
     "* Pmode : p[Enter]        *\n"
     "* Help  : (help)          *\n"
     "* Exit  :  exit           *\n"
     "* Stream: (start)         *\n"
     "---------------------------\n")
   ; load plugin
   (load-plugin)
   ; load grimoire.rc
   (load-rc)
   ; REPL
   ; dirty
    (loop [input (read-line)]
      (cond 
        (= "exit" input)
        (do 
          (try (println "bye bye!")
            (catch Exception e (println e)))
          (.shutdown @twitterstream))
        (= "e" input)
        (do
          (reset! mode :eval)
          (print "λ => ")
          (flush)
          (binding [*ns* (find-ns 'grimoire.core)]
            (try
              (println (load-string (read-line)))
              (catch Exception e (println (.getMessage e)))))
          (reset! mode :normal)
          (recur (read-line)))
        (= "p" input)
        (do
          (reset! mode :post)
          (print "Post => ")
          (flush)
          (post (read-line))
          (reset! mode :normal)
          (recur (read-line)))
        :else  
          (recur (read-line))))))
