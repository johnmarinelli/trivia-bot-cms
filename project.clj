(defproject trivia-cms "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :main trivia-cms.core
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.229"]
                 [compojure "1.5.1"] ; routes
                 [hiccup "1.0.5"] ; html 
                 [ring/ring-defaults "0.2.1"] ; server
                 [ring/ring-json "0.4.0"] ; json parser
                 [com.novemberain/monger "3.1.0"] ; mongodb client
                 [org.clojure/tools.logging "0.3.1"] ; logging
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jdmk/jmxtools
                                                    com.sun.jmx/jmxri]] ; logging
                 [environ "1.1.0"]  ; environment manager
                 [inflections "0.12.2"] ; rails like english idioms (pluralize)
                 [buddy "0.8.3"] ; auths
                 ]
  :plugins [[lein-ring "0.9.7"]
            [lein-environ "1.1.0"]
            [lein-cljsbuild "1.1.4"]
            [com.jakemccrary/lein-test-refresh "0.17.0"]]

  :ring {:handler trivia-cms.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]
                        [org.clojure/tools.nrepl "0.2.12"]
                        [org.clojure/data.json "0.2.6"]]
         :jvm-opts ["-Dlogfile.path=dev"]
         :env {:database-name "ltcla_quizzes"}
         :repl {:dependencies [[trivia-cms.handler]
                               [trivia-cms.db]]}}

   :test {:jvm-opts ["-Dlogfile.path=test"]
          :dependencies [[org.clojure/tools.nrepl "0.2.12"]
                         [javax.servlet/servlet-api "2.5"]
                         [ring/ring-mock "0.3.0"]
                         [org.clojure/data.json "0.2.6"]]
          :env {:database-name "ltcla_quizzes_test"}}

   :prod {:jvm-opts ["-Dlogfile.path=production"]
          :env {:database-name "ltcla_quizzes_prod"}}})
