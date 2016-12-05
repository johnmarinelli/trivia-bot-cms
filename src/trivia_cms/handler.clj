(ns trivia-cms.handler
  (:require [trivia-cms.api.api-frontend :refer [api]]
            [trivia-cms.api.user-login :refer [user-login]]
            [compojure.core :refer [routes defroutes ANY]]))

(defroutes all-routes
  (ANY "/api*" [] api)
  (ANY "/log(in|out)" [] user-login))

(def app
  (routes all-routes))
