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
  (println session)
  (if-let [u (user/check-user-password username password)]
    (let [uid (.toString (:_id u))]
      (user/set-token uid "1")
      (user/set-modified-at uid)
      (-> (response "1")
          (update :headers #(merge {"Set-Cookie" (str "token=" (user/get-token uid))
                                    "Username" username} %))))
    (response "0")))

(defn logout [{session :session}]
  (assoc (response "1") :session (dissoc session :identity)))

(defn is-authenticated [{cookies :cookies :as req}]
  (let [token (:value (get cookies "token"))
        username (:value (get cookies "username"))
        user (user/find-user username)
        user-token (:token user)] 
    (println token)
    (and (not (some nil? [token username user user-token])) (= token user-token))))

(defn wrap-user [handler]
  (fn [{user-id :identity :as req}]
    (handler (assoc req :user (user/find-user user-id)))))

(defroutes user-login-routes 
  (POST "/login" request
        (let [form-params (:params request)
              session (:session request)]
          (println "Params:" form-params)
          (println "Request: " request)
          (login (:username form-params) (:password form-params) session)))

  (GET "/abc" [req] (println req))

  (POST "/logout" request []
        (if (is-authenticated (:cookies request))
          (let [username (:value (get (:cookies request) "username"))]
            (user/remove-token username)
            (response "1"))
          (response "0"))))

(def backend (session-backend))

(def user-login
  (->
   user-login-routes
   (wrap-session)
   (wrap-defaults api-defaults)
   (wrap-authentication backend)
   (wrap-authorization backend)
   (wrap-user)
   (wrap-json-params)))
