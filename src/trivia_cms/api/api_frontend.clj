(ns trivia-cms.api.api-frontend
  (:require [clojure.java.io]
            [clojure.data.json :as json]

            [compojure.core :refer :all]
            [compojure.route :as route]

            [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.session :refer [wrap-session]]
            
            [buddy.auth.accessrules :refer [restrict]]



            [trivia-cms.api.user-login :refer [wrap-user]]

            [trivia-cms.models.quiz :as quiz]
            [trivia-cms.models.question :as question]
            [trivia-cms.api.user-login :as user-login]
            [trivia-cms.api.public-api :as public-api]
            [trivia-cms.trailing-slash-middleware :refer [trailing-slash-middleware]])

  (:use [ring.util.response :only [response not-found]] ; wrap json response
))

(defroutes api-routes

  (context "/api" []
           (restrict api-routes {:handler user-login/is-authenticated
                                 :on-error (fn [req val] 
                                             (println "API error - request: " req)
                                             (let [msg (doall 
                                                        (if (empty? val)  
                                                          "Unauthorized" 
                                                          val))] 
                                               (response (str "Error: " msg))))}))

  (GET "/"
       [_]
       (response "SUP"))

  (GET "/api/quizzes" 
       [request] 
       (let [res (quiz/find-models {})]
         (println request)
         (map #(public-api/serialize %) res)))

  (GET ["/api/quizzes/:name" :name #".{1,16}"]
       [name]
       (let [quizzes (quiz/find-models {:quiz-name name})]
         (if (empty? quizzes)
           (not-found {:error-message (str "Quiz '" name "' not found.")})
           (response 
            (-> (first quizzes)
                (public-api/serialize))))))

  ; /.{n,}/ is regex for "anything with n characters".
  ; since question names are capped to 16 characters, we 
  ; assume anything longer than 16 characters is a quiz id.
  (GET ["/api/quizzes/:id" :id #".{17,}"]
       [id]
       (let [api-res (quiz/find-models {:_id id})]
         (if (nil? api-res)
           (response {:error-message (str "Quiz with id '" id "' not found.")})
           (let [quiz (first api-res)] 
             (response
              (-> quiz
                  (public-api/serialize)))))))

  (POST "/api/quizzes/create" req
        (let [params (:params req)
              quiz-name (:quiz-name params)
              questions (or (:questions params) [])
              validated (not (some nil? [params quiz-name]))]
          (if (not validated)
            {:status 400 
             :body {:error-message  
                    (str "Name is required when creating quizzes. Given: " params)} }
            (response 
             (-> (quiz/create params)
                 (public-api/serialize))))))

  (POST ["/api/quizzes/:quiz-name/questions" :quiz-name #".{1,16}"]
        [quiz-name & params]
        (let [quiz (first (quiz/find-models {:quiz-name quiz-name}))
              {:keys [body answer category value]} params
              validated (not (some nil? [quiz body answer category value]))]
          (if (not validated)
            {:status 400
             :body {:error-message (str "Question could not be created.  Were all values filled out?  Is " quiz-name " a valid quiz?")}}
            (let [question (question/create params)]
              (let [new-quiz (quiz/add-questions quiz [question])]
                (response
                 (public-api/serialize new-quiz)))))))

  (POST ["/api/quizzes/:quiz-id/questions" :quiz-id #".{17,}"]
        [quiz-id & params]

        (let [quiz (first (quiz/find-models {:_id quiz-id}))
              {:keys [body answer category value]} params
              validated (not (some nil? [quiz body answer category value]))]
          (if (not validated)
            {:status 400
             :body {:error-message (str "Question could not be created.  Were all values filled out?  Is it a valid quiz?")}}
            (let [question (question/create params)]
              (let [new-quiz (quiz/add-questions quiz [question])]
                (response
                 (public-api/serialize new-quiz)))))))


  (DELETE "/api/quizzes/:id"
          [id]
          (let [num-deleted (quiz/destroy id)]
            (if (> num-deleted 0)
              (response {:num-deleted num-deleted})
              (not-found 
               (json/write-str {:error-message (str "Quiz '" id "' not found.")})))))

  (DELETE ["/api/quizzes/:quiz-name/questions/:question-id" 
           :quiz-name #".{1,16}"]
          [quiz-name question-id]
          (let [quiz (first (quiz/find-models {:quiz-name quiz-name}))]
            (if (not (nil? quiz))
              (let [res (quiz/remove-questions quiz [question-id])]
                (response 
                 (public-api/serialize res)))
              {:status 400
               :body {:error-message (str "Quiz " quiz-name " not found.")}})))

  (DELETE ["/api/quizzes/:quiz-id/questions/:question-id" :quiz-id #".{17,}"] 
          [quiz-id question-id]
          (let [quiz (first (quiz/find-models {:_id quiz-id}))]
            (if (not (nil? quiz))
              (let [res (quiz/remove-questions quiz [question-id])]
                (response 
                 (public-api/serialize res)))
              {:status 400
               :body {:error-message (str "Quiz " quiz-id " not found.")}}))))

(def api
  (->
   api-routes
   (wrap-defaults api-defaults)
   (wrap-session)
   (wrap-user)


   (wrap-json-params)
   (wrap-json-response)
   (trailing-slash-middleware)))
