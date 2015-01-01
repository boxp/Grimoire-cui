(ns grimoire.data)

; twitter
(def oauthtoken
  (atom nil))
(def tweets 
  "Received status vector"
  (ref [])) 
(def mentions
  "Received mentions (deplicated)"
  (ref []))
(def friends 
  "Received friends list (deplicated)"
  (ref #{}))
(def myname
  "My twitter screen name"
  (atom nil))

; plugin
(def plugins 
  "プラグインが収納される集合"
  (ref #{}))

(defprotocol Plugin
  "Grimoireのプラグインを示すプロトコル，プラグインを作るにはreify,proxyを用いて継承し，pluginsに追加して下さい．" 
  (get-name [this])
  (on-status [this status])
  (on-rt [this status])
  (on-unrt [this status])
  (on-fav [this source target status])
  (on-unfav [this source target status])
  (on-del [this status])
  (on-follow [this source user])
  (on-dm [this dm])
  (on-start [this])
  (on-click [this e]))

; system
(def nrepl-server 
  "Var nrepl-server"
  (atom nil))
(def browser 
  "Var to use browser"
  (atom "w3m"))
(def mode
  "現在のmodeを示す :normal 通常 :eval Evalモード :post 投稿モード"
  (atom :normal))
(def out-cache
  "出力のキャッシュ，後で出力したい場合に使う"
  (ref []))
