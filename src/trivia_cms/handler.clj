(ns trivia-cms.handler
  (:require [trivia-cms.api.api-frontend :refer [api]]
            [trivia-cms.api.user-login :refer [user-login]]
            [compojure.core :refer [routes defroutes ANY POST]]))

(defroutes all-routes
  (ANY "/api*" [] api)
  (POST "/login" [] user-login)
  (POST "/logout" [] user-login))

(def app
  (routes all-routes))
