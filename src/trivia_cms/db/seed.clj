(ns trivia-cms.db.seed
  (:require [trivia-cms.models.user :refer [create-user!]]
            [environ.core :refer [env]]))

(defn load-fixtures []
  (create-user! {:username (env :ltcla-trivia-cms-username)
                 :password (env :ltcla-trivia-cms-password)}))
