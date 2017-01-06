(ns towerbattles.core
  (:require [reagent.core :as r]
            [towerbattles.astar :as astar]))

(enable-console-print!)

(defn distance-sq [[y1 x1] [y2 x2]]
  (+ (Math/pow (Math/abs (- y2 y1)) 2)
     (Math/pow (Math/abs (- x2 x1)) 2)))

(defn distance [start end]
  (Math/sqrt (distance-sq start end)))

(defn get-neighbors [towers [y x]]
  (concat [[-1 0] [1 0] [0 -1] [0 1]]
          #_
          (remove (fn [[dy dx]]
                    (and (get-in towers [(+ y dy) x])
                         (get-in towers [y (+ x dx)])))
                  [[-1 -1] [-1 1] [1 1] [1 -1]])))

(defn out? [[y x]]
  (not (and (<= 0 x 8)
            (<= -1 y 17))))

(defn move [[y x] [dy dx]]
  [(+ y dy) (+ x dx)])

(defn refresh-route [{:keys [towers] :as app-state}]
  (assoc app-state :route (time (astar/astar {:heurestic distance
                                              :distance distance
                                              :get-neighbors (fn [current closed?]
                                                               (->> (get-neighbors towers current)
                                                                    (map (partial move current))
                                                                    (remove closed?)
                                                                    (remove #(get-in towers %))
                                                                    (remove out?)))
                                              :start [-1 4]
                                              :goal [17 4]}))))

(def empty-state (refresh-route {:towers {}}))

(defonce app-state (r/atom empty-state))

(defn reset-game []
  (reset! app-state empty-state))

(defn update-tower [app-state y x f]
  (let [maybe-new-state (-> app-state
                            (update-in [:towers y x] f)
                            refresh-route)]
    (if (:route maybe-new-state)
      maybe-new-state
      app-state)))

(defn toggle-tower [app-state y x]
  (update-tower app-state y x (fn [state]
                                (if (nil? state)
                                  true))))

(defn segment-points [[y1 x1] [y2 x2]]
  (let [dy (- y2 y1)
        dx (- x2 x1)
        py (or (/ dy dx) 0)
        px (or (/ dx dy) 0)]
    (if (> (Math/abs dy) (Math/abs dx))
      (let [f (if (pos? dy) + -)]
        (map (fn [y]
               [(f y1 y) (Math/round (f x1 (* y px)))])
             (range (inc (Math/abs dy)))))
      (let [f (if (pos? dx) + -)]
        (map (fn [x]
               [(Math/round (f y1 (* x py))) (f x1 x)])
             (range (inc (Math/abs dx))))))))

(defn drag-drop-start [current]
  (swap! app-state assoc :mouse {:start current}))

(defn drag-drop-hover [current]
  (swap! app-state update :mouse (fn [{:keys [start] :as mouse}]
                                   (assoc mouse :hover current :path (segment-points start current)))))

(defn maybe-build-towers [app-state segment]
  (let [first-has-tower? (get-in (:towers app-state) (first segment))]
    (reduce (fn [app-state [y x]]
              (update-tower app-state y x (fn [state]
                                            (if first-has-tower?
                                              nil
                                              true))))
            app-state
            segment)))

(defn drag-drop-end [current]
  (swap! app-state (fn [app-state]
                     (-> app-state
                         (maybe-build-towers (segment-points (:start (:mouse app-state)) current))
                         refresh-route
                         (assoc :mouse nil)))))

(defn drag-drop-cancel []
  (swap! app-state assoc :mouse nil))

(defn game-board []
  (let [{:keys [towers route mouse]} @app-state
        route (set route)
        first-has-tower? (get-in towers (first (:path mouse)))
        mouse-path (set (:path mouse))]
    [:table.game-board
     [:thead
      [:tr
       {:key "start"}
       [:td.game-board__start {:col-span 9}]]]
     [:tbody
      (for [y (range 16)]
        [:tr
         {:key y}
         (for [x (range 9)]
           [:td.game-board__cell
            {:key x
             :class (str (if (get-in towers [y x])
                           "game-board__cell--tower "
                           "game-board__cell--empty ")
                         (if (contains? route [y x])
                           "game-board__cell--route ")
                         (if (contains? mouse-path [y x])
                           (if first-has-tower?
                             "game-board__cell--selected-remove "
                             "game-board__cell--selected " )))
             :on-click #(swap! app-state toggle-tower y x)
             :on-mouse-down (fn [e]
                              (.preventDefault e)
                              (if (or (= 3 (.-which e)) (= 2 (.-button e)))
                                (drag-drop-cancel)
                                (drag-drop-start [y x])))
             :on-mouse-enter (fn [e]
                               (when mouse
                                 (drag-drop-hover [y x])))
             :on-mouse-up (fn [e]
                            (drag-drop-end [y x]))}
            ""])])]
     [:tfoot
      [:tr
       {:key "end"}
       [:td.game-board__end {:col-span 9}]]]]))

(defn tools []
  [:div.tools
   [:button.button
    {:type "button"
     :on-click #(reset-game)}
    "Reset"]

   [:ul
    [:li [:strong "Path length: "] (count (:route @app-state))]]])

(defn help []
  [:div.help
   [:h2 "Controls"]
   [:ul
    [:li "Click on cell to toggle wall."]
    [:li "Drag & drop to build or remove segment of wall."]]])

(defn menu []
  [:div.menu
   [tools]
   [help]])

(defn main []
  [:div.game-view
   {:on-context-menu (fn [e]
                       (.preventDefault e))}
   [:div.board-container [game-board]]
   [menu]])

(defn on-js-reload []
  (r/render [main] (js/document.getElementById "app")))

(on-js-reload)
