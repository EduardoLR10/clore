(ns clojure-demo.http
  (:require
   [clj-simple-router.core :as router]
   [clojure-demo.datomic :as datomic]
   [clojure-demo.renderer :as renderer]
   [datomic.api :as d]
   [org.httpkit.server :as httpkit]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.resource :refer [wrap-resource]]
   [rum.core :as rum]))

(defn stop! [server]
  (some-> server httpkit/server-stop! deref))

(def ^{:init/tags #{::httpkit/config}} config
  {:port 8080
   :legacy-return-value? false})

(defn server
  "The actual HTTP server.

  Note that HTTP Kit doesn't actually support HTTPS. You'll want to set up
  some reverse proxy for that, or perhaps switch to a different HTTP server."
  {:init/tags #{:http/server}
   :init/inject [:ring/handler ::httpkit/config]
   :init/stop-fn #'stop!}
  [handler config]
  (httpkit/run-server handler config))

(defn middleware
  "Ring middleware to be applied to the handler.
  Here is the appropriate place to address cross-cutting concerns, such as
  authentication, making the database available to the handler, etc."
  {:init/tags #{:ring/middleware}
   :init/inject [:datomic/conn]}
  [conn]
  (fn [base-handler]
    (-> base-handler
        (wrap-resource "css")
        ((fn [handler]
           (fn [req]
             (-> req
                 (update :form-params update-keys keyword)
                 handler))))
        (wrap-params)
        ((fn [handler]
           (fn [req]
             (handler (assoc req :datomic {:conn conn
                                           :db (d/db conn)}))))))))

(defn handler
  "Constructs the actual handler for the requests.
  Uses the routing information and any additional middleware."
  {:init/tags #{:ring/handler}
   :init/inject [::routes :ring/middleware]}
  [routes middleware]
  (-> routes
      router/router
      middleware))

(def ^:init/name routes
  "Here is where we define the route-specific handlers.
  Since we're using clj-simple-router, this is just a map."
  {"GET /"
   (fn [{{:keys [db]} :datomic}]
     {:status 200
      :body
      (let [current-date (new java.util.Date)]
      (rum/render-static-markup
       (concat
        (renderer/common-header "/")
        (list (renderer/title "All Reminders"))
        (renderer/short-term-reminders "/" db current-date)
        (renderer/long-term-reminders "/" db current-date))))})
   "GET /short-term"
   (fn [{{:keys [db]} :datomic}]
     {:status 200
      :body
      (rum/render-static-markup
       (concat
        (renderer/common-header "/short-term")
        (list (renderer/title "Short Term Reminders"))
        (renderer/short-term-reminders "/short-term" db (new java.util.Date))))})
   "GET /long-term"
   (fn [{{:keys [db]} :datomic}]
     {:status 200
      :body
      (rum/render-static-markup
       (concat
        (renderer/common-header "/long-term")
        (list (renderer/title "Long Term Reminders"))
        (renderer/long-term-reminders "/long-term" db (new java.util.Date))))})
   "POST /add-reminder"
   (fn [{{:keys [conn]} :datomic
         {:keys [path] :as body} :form-params}]
     (prn body)
     (datomic/add-reminder conn body (new java.util.Date))
     {:status 302
      :headers {"Location" path}
      :body ""})
   "POST /delete-reminder/*"
   (fn [{{:keys [conn]} :datomic
         {:keys [path]} :form-params
         [id] :path-params}]
     (datomic/delete-reminder conn (parse-uuid id))
     {:status 302
      :headers {"Location" path}
      :body ""})})
