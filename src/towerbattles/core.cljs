(ns towerbattles.core
  (:require [reagent.core :as r]
            [towerbattles.astar :as astar]
            [towerbattles.utils :as utils]
            [goog.dom :as dom]))

(enable-console-print!)

(defn distance-sq [[y1 x1] [y2 x2]]
  (+ (Math/pow (Math/abs (- y2 y1)) 2)
     (Math/pow (Math/abs (- x2 x1)) 2)))

(defn distance [start end]
  (Math/sqrt (distance-sq start end)))

(defn heurestic [[y x]]
  (- 16 y))

(defn get-neighbors [towers [y x]]
  (concat [[-1 0] [1 0] [0 -1] [0 1]]
          #_
          (remove (fn [[dy dx]]
                    (and (get-in towers [(+ y dy) x])
                         (get-in towers [y (+ x dx)])))
                  [[-1 -1] [-1 1] [1 1] [1 -1]])))

(defn out? [[y x]]
  (not (and (<= 0 x 8)
            (<= -1 y 16))))

(defn move [[y x] [dy dx]]
  [(+ y dy) (+ x dx)])

(defn goal? [[y x]]
  (= y 16))

(defn refresh-route [{:keys [towers] :as app-state}]
  (let [route (time (astar/astar {:heurestic heurestic
                                  :distance distance
                                  :get-neighbors (fn [current closed?]
                                                   (->> (get-neighbors towers current)
                                                        (map (partial move current))
                                                        (remove closed?)
                                                        (remove #(get-in towers %))
                                                        (remove out?)))
                                  :start [-1 4]
                                  :goal? (fn [[y x]]
                                           (= y 16))}))
        start (first (filter (fn [[y x]] (= 0 y)) (reverse route)))
        ;; only used for display
        end (second route)
        route-map (first (reduce (fn [[acc next] node]
                                   [(if next
                                      (assoc acc node next)
                                      acc)
                                    node])
                                 [{} nil]
                                 route))]
    (assoc app-state
           :start start
           :end end
           :route route
           :route-map route-map)))

(def empty-state (refresh-route {:towers {}
                                 :mobs []
                                 :mob-id 0
                                 :tick 0
                                 :lives 50}))

(defonce app-state (r/atom empty-state))

(defn reset-game []
  (reset! app-state empty-state))

(defn send-mob []
  (swap! app-state (fn [state]
                     (-> state
                         (update :mob-id inc)
                         (update :mobs conj {:id (:mob-id state)
                                             :pos (update (:start state) 0 dec)})))))

(defn move-mob [route-map {:keys [pos] :as mob}]
  (assoc mob :pos (get route-map pos pos)))

(defn move-mobs [{:keys [mobs route-map] :as app-state}]
  (assoc app-state :mobs (vec (map (partial move-mob route-map) mobs))))

(defn mobs-on-goal [{:keys [mobs] :as app-state}]
  (let [on-goal (vec (filter (comp goal? :pos) mobs))
        mobs (vec (remove (comp goal? :pos) mobs))]
    (-> app-state
        (update :lives - (count on-goal))
        (assoc :mobs mobs))))

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
                                   (js/console.log mouse)
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
                     (if (:mouse app-state)
                       (-> app-state
                           (maybe-build-towers (segment-points (:start (:mouse app-state)) current))
                           refresh-route
                           (assoc :mouse nil))
                       app-state))))

(defn drag-drop-cancel []
  (swap! app-state assoc :mouse nil))

(defn game-board []
  (let [t-ref (atom nil)
        t-ref-fn #(reset! t-ref %)]
    (fn []
      (let [{:keys [towers route mouse mobs start end]} @app-state
            route (set route)
            first-has-tower? (get-in towers (first (:path mouse)))
            mouse-path (set (:path mouse))]
        [utils/window-event-listener
         ;; Any mouse even outside game-board cancels drag&drop
         {:on-mouse-over (fn [e]
                           (if (not (dom/contains @t-ref (.-target e)))
                             (drag-drop-cancel)))}
         [:div.game-board-container
          [:table.game-board
           {:ref t-ref-fn}
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
                                   "game-board__cell--selected " ))
                               (if (= [y x] start)
                                 "game-board__cell--start ")
                               (if (= [y x] end)
                                 "game-board__cell--end ") )
                   :on-click #(swap! app-state toggle-tower y x)
                   :on-mouse-down (fn [e]
                                    (.preventDefault e)
                                    (if (= 2 (.-button e))
                                      (drag-drop-cancel)
                                      (drag-drop-start [y x])))
                   :on-mouse-enter (fn [e]
                                     (when mouse
                                       (drag-drop-hover [y x])))
                   :on-mouse-up (fn [e]
                                  (drag-drop-end [y x]))}
                  ""])])]]
          [:div.mob-container
           (for [{:keys [id pos]} mobs]
             [:div.mob
              {:key id
               :style {:top (str (inc (* 2 (first pos))) "rem")
                       :left (str (inc (* 2 (second pos))) "rem")}}])]]]))))

(defn board-container []
  [:div.board-container
   [game-board]])

(defn tools []
  [:div.tools
   [:button.button
    {:type "button"
     :on-click #(reset-game)}
    "Reset"]

   [:button.button
    {:type "button"
     :on-click #(send-mob)}
    "Send"]

   [:ul
    [:li [:string "Lives: "] (:lives @app-state)]
    [:li [:strong "Tick: "] (:tick @app-state)]
    [:li [:strong "Path length: "] (count (:route @app-state))]]])

(defn help []
  [:div.help
   [:h2 "Controls"]
   [:ul
    [:li "Click on cell to toggle wall."]
    [:li "Drag & drop to build or remove segment of wall."]
    [:li "Towers that would block mobs, can't be build."]]])

(defn menu []
  [:div.menu
   [tools]
   [help]])

(defn main []
  [:div.game-view
   {:on-context-menu (fn [e]
                       (.preventDefault e))}
   [board-container]
   [menu]])

(defn tick []
  (swap! app-state (fn [state]
                     (-> state
                         (update :tick inc)
                         mobs-on-goal
                         move-mobs))))

(defonce game-loop (atom nil))

(defn on-js-reload []
  (r/render [main] (js/document.getElementById "app"))
  (swap! game-loop (fn [x]
                     (if x (js/clearInterval x))
                     (js/setInterval tick 1000))))

(on-js-reload)
