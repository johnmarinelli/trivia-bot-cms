(ns trivia-cms.api.user-login
  (:require [trivia-cms.models.user :as user]
            [trivia-cms.trailing-slash-middleware :refer [trailing-slash-middleware]]
            [ring.util.response :refer [response redirect]]
            [ring.middleware.json :refer [wrap-json-params]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [compojure.core :refer :all]
            [buddy.auth.backends.session :refer [session-backend]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.params :refer [wrap-params]]))

(defn login [username password session]
  (println username password)
  (if-let [user (user/check-user-password username password)]
    (assoc (response "1") :session (assoc session :identity (:id user)))
    (response "0")))

(defn logout [{session :session}]
  (assoc (response "1") :session (dissoc session :identity)))

(defn is-authenticated [{user :user :as req}]
  (not (nil? user)))

(defn wrap-user [handler]
  (fn [{user-id :identity :as req}]
    (handler (assoc req :user (user/find-user user-id)))))

(defroutes user-login-routes 
  (POST "/login" request
        (let [form-params (:params request)
              session (:session request)]
          (login (:username form-params) (:password form-params) session)))
  (POST "/logout" [] logout))

(def backend (session-backend))

(def user-login
  (->
   user-login-routes
   (wrap-defaults api-defaults)
   (wrap-json-params)
   (wrap-user)
   (wrap-authentication backend)
   (wrap-authorization backend)
   (wrap-session)))
