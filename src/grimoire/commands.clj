(ns grimoire.commands
  (:use [clojure.java.shell]
        [clojure.string :only [join split]]
        [clojure.repl :as repl]
        [clojure.java.io]
        [grimoire.data]
        [grimoire.oauth])
  (:require [net.cgrand.enlive-html :as en])
  (:import (twitter4j TwitterFactory Query Status User UserMentionEntity)
           (twitter4j.auth AccessToken)
           (twitter4j StatusUpdate)
           (java.io File)))


; コマンドたち
; ツイート
(defn post 
  "引数の文字列を全て一つにまとめてツイートする．140文字以上の時は省略されます．"
  [& input]
  (try 
    (str "Success:" 
      (.getText 
        (.updateStatus twitter 
          (if 
            (> (count (apply str input)) 140)
              (apply str (take 137 (apply str input)) "...")
              (apply str input)))))
    (catch Exception e (str "Something has wrong." (.getMessage e)))))

; 20件のツイート取得
(defn showtl 
  " Showing 20 new tweets from HomeTimeline.\nUsage: (showtl)"
  []
  (let [statusAll (reverse (.getHomeTimeline twitter))]
    (loop [status statusAll i 1]
      (if (= i 20)
        nil
        (do
          (println (.getScreenName (.getUser (first status))) ":" (.getText (first status)))
          (recur (rest status) (+ i 1)))))))

; コマンド一覧
(defn help []
  (str     "*** Grimoire-cli Commands List***\n\n"
           "post: ツイート(例：(post \"test\"))\n"
           "start: ユーザーストリームをスタートさせる.\n"
           "stop: ユーザーストリームをストップさせる.\n"
           "fav: ふぁぼる(例：(fav 2))\n"
           "retweet: リツイートする(例：(ret 3))\n"
           "favret: ふぁぼってリツイートする(例：(favret 6))\n"
           "reply: リプライを送る(例：(reply 1 \"hoge\"))\n"
           "del: ツイートを削除(例：(del 168))\n"
           "autofav!: 指定したユーザーのツイートを自動でふぁぼる(例：(autofav! \"@If_I_were_boxp\"))\n"
           "follow: ツイートのユーザーをフォローする(例：(follow 58))\n"
           "Get more information to (doc <commands>)."))

; リツイート
(defn ret
  "statusnum(ツイートの右下に表示)を指定してリツイート"
  [statusnum]
  (try 
    (let [status (.retweetStatus twitter (.getId (@tweets statusnum)))]
      (str 
        "Success retweet: @" 
        (.. status getUser getScreenName)
        " - "
        (.. status getText))
      statusnum)
    (catch Exception e "something has wrong.")))

; リツイートの取り消し
(defn unret
  "statusnum(ツイートの右下に表示)を指定してリツイートを取り消し"
  [statusnum]
  (try 
    (let [status (.destroyStatus twitter (.getId (@tweets statusnum)))]
      (str 
        "Success unretweet: @" 
        (.. status getUser getScreenName)
        " - "
        (.. status getText))
      statusnum)
    (catch Exception e "something has wrong.")))

; ふぁぼふぁぼ
(defn fav
  "statusnum(ツイートの右下に表示)を指定してふぁぼ"
  [statusnum]
  (try
    (let [status (.createFavorite twitter (.getId (@tweets statusnum)))]
      (str
        "Success Fav: @" 
        (.. status getUser getScreenName)
        " - "
        (.. status getText))
      statusnum)
    (catch Exception e "something has wrong.")))

; あんふぁぼ
(defn unfav
  "statusnum(ツイートの右下に表示)を指定してあんふぁぼ"
  [statusnum]
  (try
    (let [status (.destroyFavorite twitter (.getId (@tweets statusnum)))]
      (str
        "Success UnFav: @" 
        (.. status getUser getScreenName)
        " - "
        (.. status getText))
      statusnum)
    (catch Exception e "something has wrong.")))

; ふぁぼRT
; clean
(defn favret 
  "statusnum(ツイートの右下に表示)を指定してふぁぼ＆リツイート"
  [statusnum]
  (-> statusnum fav ret))

; ふぁぼRT
; clean
(defn unfavret 
  "statusnum(ツイートの右下に表示)を指定してふぁぼ＆リツイートを取り消す"
  [statusnum]
  (-> statusnum unfav unret))

; つい消し
(defn del
  "statusnum(ツイートの右下に表示)を指定してツイートを取り消す"
  [statusnum]
  (try 
    (let [status (@tweets statusnum)]
      (.destroyStatus twitter (.getId status))
      (str 
        "Success delete: @" 
        (.. status getUser getScreenName)
        " - "
        (.. status getText))
      statusnum)
    (catch Exception e "something has wrong.")))

; リプライ
(defn reply [statusnum & texts]
  "statusnum(ツイートの右下に表示)とテキストを指定して,返信する"
  (let [reply (str \@ (.. (@tweets statusnum) getUser getScreenName) " " (apply str texts))]
    (do
      (println (str (apply str (take 137 (seq reply)))))
      (str "Success:" 
        (.getText
          (.updateStatus 
            twitter 
            (doto
              (StatusUpdate. 
                (if 
                  (> (count (seq reply)) 140)
                  (str (apply str (take 137 (seq reply))) "...")
                  (str \@ (.. (@tweets statusnum) getUser getScreenName) " " (apply str texts))))
              (.inReplyToStatusId (.getId (@tweets statusnum))))))))))

; get-source from html
(defn get-source
  "twitter4j.Status.getSourceのhtmlをテキストに変換"
  [source]
  (first 
    (:content 
      (first 
        (:content 
          (first 
            (:content
              (first
                (en/html-resource 
                    (java.io.StringReader.  source))))))))))


(defn autofav!
  "引数のユーザーを自動でふぁぼる(マジキチ向け)"
  [& user]
  (let [plugin (reify Plugin
                 (on-status [_ status] 
                   (if (some #(= %  (.. status getUser getScreenName)) user) 
                    (fav 
                      (.indexOf @tweets status)))))]
    (dosync
      (alter plugins conj plugin))))

(defn get-home
  "ホームディレクトリを取得"
  []
  (let [home (System/getenv "HOME")]
    (if home
      home
      (System/getProperty "user.home"))))

(defn search
  "引数の文字列からツイートを検索し，twitter4j.Statusで返す．"
  [& strs]
    (reverse
      (.. twitter (search (Query. (apply str (join " " strs)))) getTweets)))

(defn w3m
  "w3mでurlを見る"
  [url]
  (sh "w3m" url))

(defn url
  "statusnum(ツイートの右下に表示)を指定して，ツイートの中のURLをWebViewでブラウズ"
  [statusnum] 
  (let [status (@tweets statusnum)
        text (split (.getText status) #" ")
        urls (filter #(= (seq "http") (take 4 %)) text)]
    (doall
      (map #(w3m %) urls))))
 

(defn browse
  "statusnum(ツイートの右下に表示)を指定して，ツイートの中のURLをbrowserに指定したブラウザでブラウズ"
  [statusnum] 
  (let [status (@tweets statusnum)
        text (split (.getText status) #" ")
        urls (filter #(= (seq "http") (take 4 %)) text)]
    (doall
      (map #(sh @browser %) urls))))

(defn follow
  "statusnum(ツイートの右下に表示)を指定して，指定したツイートのユーザーをフォローする"
  [statusnum]
  (let [status (@tweets statusnum)]
    (if (.isRetweet status)
      (.createFriendship twitter (.. status getRetweetedStatus getUser getId) true)
      (.createFriendship twitter (.. status getUser getId) true))
    statusnum))

(defn vim
  "vimを立ち上げ，保存した文字列をEvalします．(引数を指定するとファイル名を指定)"
  ([]
    (binding [*ns* (find-ns 'grimoire.core)]
      (future 
        (try
          (do
            (sh "vim" 
              (str (get-home) "/.grimoire/.tmp")))
            (println
              (load-file 
                (str (get-home) "/.grimoire/.tmp")))
          (catch Exception e (println e))))))
  ([adr]
    (binding [*ns* (find-ns 'grimoire.core)]
      (future 
        (try
          (do
            (sh "vim" adr))
            (println
              (load-file adr))
          (catch Exception e (println e)))))))

(defn print-later!
  "normalモード時にまとめて表示するようキャッシュし，normalモードであれば表示する．バックグラウンドで表示する際はこの関数を使って下さい．"
  [& string]
  (if (= @mode :normal)
    (let [output (conj @out-cache (apply str string))]
      (do
        (doall (map println output))
        (dosync
          (alter out-cache empty))))
    (dosync
      (alter out-cache conj (apply str string)))))
  
; デバック用
(defn reload 
  ([]
    "プロジェクトのソースコードをリロードします．(デバック用)"
    (do
      (load-file (str (get-home) "/.grimoire.clj"))
      (load-file (str (get-home) "/Dropbox/program/clojure/grimoire-cli/src/grimoire/commands.clj"))
      (load-file (str (get-home) "/Dropbox/program/clojure/grimoire-cli/src/grimoire/plugin.clj"))
      (use 'grimoire.commands)
      (use 'grimoire.plugin)))
  ([file]
    (do
      (load-file (str (get-home) "/Dropbox/program/clojure/grimoire-cli/src/grimoire/" file ".clj")))))
