(ns grimoire.listener
  (:import (twitter4j UserStreamListener))
  (:use [grimoire.oauth]
        [clojure.string :only [split]]
        [grimoire.data]
        [grimoire.commands]))

; Userstream status listener (console)
(defn listener []
  (reify UserStreamListener
    (onStatus [this status]
      (let [_ (dosync
                (alter tweets conj status)) ; ツイートを追加
            output (str 
                     (.indexOf @tweets status)
                     " "
                     (.. status getUser getScreenName)
                     ":"
                     (.. status getText)
                     " "
                     (str (.. status getCreatedAt))
                     " "
                     (get-source (.. status getSource)))]
        (do
          ; メンションツイート？
          (if (some #(= @myname %) (map #(.getText %) (.. status getUserMentionEntities)))
            ; メンションツイート処理
            (do
              (print-later! (str "-> " output))
              (dosync
                (alter mentions conj status))
              ; 通知音
              (ring!)
              ; プラグイン処理(on-reply)
              (try
                (doall
                  (map #(.on-reply % status) @plugins))
                (catch Exception e (print-later! (.getMessage e)))))
            (do
              ; プラグイン処理(on-status)
              (try
                (doall
                  (map #(.on-status % status) @plugins))
                (catch Exception e (print-later! (.getMessage e))))
              ; ツイート処理
              (print-later! output))))))

    (onDeletionNotice [this statusDeletionNotice]
      (do
        (print-later!
          "Got a status deletion notice id:" 
          (.. statusDeletionNotice getStatusId))
        (try
          (doall
            (map #(.on-del % statusDeletionNotice) @plugins))
          (catch Exception e (print-later! (.getMessage e))))))

    (onTrackLimitationNotice [this numberOfLimitedStatuses]
      (do
        (print-later!
          "Got a track limitation notice:" 
          numberOfLimitedStatuses)))

    (onScrubGeo [this userId upToStatusId]
      (do
        (print-later!
          "Got scrub_geo event userId:" 
          userId 
          "upToStatusId:" 
          upToStatusId)))

    (onStallWarning [this warning]
      (do
        (print-later!
          "Got stall warning:" 
          warning)))

    (onFriendList [this friendIds]
      (do
        (dosync
          (alter friends conj friendIds))))

    (onFavorite [this source target favoritedStatus]
      (do
        ; 通知音
        (ring!)
        ; プラグインの実行
        (try
          (doall
            (map #(.on-fav % source target favoritedStatus) @plugins))
          (catch Exception e (print-later! (.getMessage e))))
        ; 出力
        (print-later!
          "You Gotta Fav! source:@" 
          (.getScreenName source) 
          " target:@" 
          (.getScreenName target) 
          " @ " 
          (.. favoritedStatus getUser getScreenName) 
          " -" 
          (.getText favoritedStatus))))

    (onUnfavorite [this source target unfavoritedStatus]
      (do
        ; プラグインの実行
        (try
          (doall
            (map #(.on-unfav % source target unfavoritedStatus) @plugins))
          (catch Exception e (print-later! (.getMessage e))))
        ; 出力
        (print-later!
          "Catched unFav! source:@" 
          (.getScreenName source) 
          " target:@" 
          (.getScreenName target) 
          " @ " 
          (.. unfavoritedStatus getUser getScreenName) 
          " -" 
          (.getText unfavoritedStatus))))

    (onFollow [this source followedUser]
      (do
        ; 通知音
        (ring!)
        ; プラグインの実行
        (try 
          (doall
            (map #(.on-follow % source followedUser) @plugins))
          (catch Exception e (print-later! (.getMessage e))))
        ; 出力
        (print-later!
          "onFollow source:@" 
          (.getScreenName source) 
          " target:@" 
          (.getScreenName followedUser))))

    (onDirectMessage [this directMessage]
      (do
        ; 通知音
        (ring!)
        ; プラグインの実行
        (try
          (doall
            (map #(.on-follow % directMessage) @plugins))
          (catch Exception e (print-later! (.getMessage e))))
        ; 出力
        (print-later!
          "onDirectMessage text:" 
          (.getText directMessage))))

    (onUserListMemberAddition [this addedMember listOwner alist]
      (print-later!
        (.getScreenName addedMember) 
        "listOwner:@" 
        (.getScreenName listOwner) 
        "list:" 
        (.getName alist)))
      
    (onUserListMemberDeletion [this deletedMember listOwner alist]
      (print-later!
        (.getScreenName deletedMember) 
        "listOwner:@" 
        (.getScreenName listOwner) 
        "list:" 
        (.getName alist)))

    (onUserListSubscription [this subscriber listOwner alist]
      (print-later!
        "onUserListSubscribed subscriber:@" 
        (.getScreenName subscriber) 
        " listOwner:@"
        (.getScreenName listOwner) 
        "list:" 
        (.getName alist)))

    (onUserListUnsubscription [this subscriber listOwner alist]
      (print-later!
        "onUserListUnSubscribed subscriber:@" 
        (.getScreenName subscriber) 
        " listOwner:@"
        (.getScreenName listOwner) 
        "list:" 
        (.getName alist)))

    (onUserListCreation [this listOwner alist]
      (print-later!
        "onUserListCreated listOwner:@"
        (.getScreenName listOwner)
        " list:"
        (.getName alist)))

    (onUserListUpdate [this listOwner alist]
      (print-later!
        "onUserListUpdated listOwner:@"
        (.getScreenName listOwner)
        " list:"
        (.getName alist)))

    (onUserListDeletion [this listOwner alist]
      (print-later!
        "onUserListDestroyed listOwner:@"
        (.getScreenName listOwner)
        " list:"
        (.getName alist)))

    (onUserProfileUpdate [this updatedUser]
      (do
        (print-later!
          "onUserProfileUpdated user:@"
          (.getScreenName updatedUser))))

    (onBlock [this source blockedUser]
      (do
        ; 通知音
        (ring!)
        (print-later!
          "onBlock user:@"
          (.getScreenName source)
          " target:@"
          (.getScreenName blockedUser))))

    (onUnblock [this source unblockedUser]
      (do
        (print-later!
          "onUnBlock user:@"
          (.getScreenName source)
          " target:@"
          (.getScreenName unblockedUser))))

    (onException [this ex]
      (print-later!
        "onException:"
        (.getMessage ex)))))
