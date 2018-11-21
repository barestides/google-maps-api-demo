(ns google-maps-api-demo.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [clojure.string :as string]))

(enable-console-print!)

(defonce app-state (r/atom {:origin ""
                            :destination ""}))


;;These contain JS objects that are initialized when the page is loaded
(def directions-service-atom (atom nil))
(def directions-display-atom (atom nil))

(defn titleize
  [str-or-kw]
  (string/capitalize (name str-or-kw)))

(defn text-box-on-change-fn
  [field]
  (fn [new-val]
    (swap! app-state assoc field (-> new-val
                                 .-target
                                 .-value))))

(defn text-box
  [field]
  [:div.form-group
   [:label {:for field} (titleize field)]
   [:input.form-control {:id field :type "text" :value (field @app-state)
                                 :on-change (text-box-on-change-fn field)}]])

(defn submit-form
  []
  (prn "we submitting"))

(defn location-form
  []
  [:form.col-sm-6 {:on-submit (fn [event]
                                ;;so the page doesn't refresh when the user submits
                                (do
                                  (.preventDefault event)
                                  (submit-form)))}
   [text-box :origin]
   [text-box :destination]
   [:input.btn.btn-primary {:type :submit :value "Submit"}]])

(defn init-map
  []
  (let [directions-service (google.maps.DirectionsService.)
        directions-display (google.maps.DirectionsRenderer.)
        map (google.maps.Map. (.getElementById js/document "map")
                 (clj->js {:zoom 7
                           :center {:lat 33.74 :lng -84.38}}))]
    (.setMap directions-display map)
    (reset! directions-service-atom directions-service)
    (reset! directions-display-atom directions-display)))

(defn map-elm
  []
  (r/create-class
   {:reagent-render (fn [] [:div#map.col-sm-6])
    :component-did-mount (fn [this] (init-map))}))

(defn page
  []
  [:div.container
   [:h2 "Google Maps API CLJS Example App"]
   [:div.row [location-form]]
   [:div.row [map-elm]]])

(defn on-js-reload []
  ;; optionally touch your app-app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-app-state update-in [:__figwheel_counter] inc)
  (r/render [page] (.getElementById js/document "app")))
