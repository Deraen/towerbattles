(ns towerbattles.astar)

(defn- construct-path [came-from current]
  (loop [current current
         path [current]]
    (if-let [next (get came-from current)]
      (recur next (conj path next))
      path)))

(defn- get-best-node [f-score open?]
  (first (reduce (fn [[current score] this]
                   (let [this-score (get f-score this)]
                     (if (or (nil? current)
                             (< this-score score))
                       [this this-score]
                       [current score])))
                 [nil nil]
                 open?)))

(defn astar [{:keys [heurestic distance get-neighbors start goal?]}]
  (loop [open? #{start}
         closed? #{}
         g-score {start 0}
         f-score {start (heurestic start)}
         came-from {}]
    (if (empty? open?)
      nil
      (let [current (get-best-node f-score open?)]
        (if (goal? current)
          (construct-path came-from current)
          (let [closed? (conj closed? current)
                [open? g-score f-score came-from _]
                (loop [open? (disj open? current)
                       g-score g-score
                       f-score f-score
                       came-from came-from
                       neighbors (get-neighbors current closed?)]
                  (if-let [neighbor (first neighbors)]
                    (let [t-score (+ (get g-score current) (distance current neighbor))]
                      (if (<= t-score (get g-score neighbor))
                        (recur (conj open? neighbor)
                               g-score
                               f-score
                               came-from
                               (rest neighbors))
                        (recur (conj open? neighbor)
                               (assoc g-score neighbor t-score)
                               (assoc f-score neighbor (+ t-score (heurestic neighbor)))
                               (assoc came-from neighbor current)
                               (rest neighbors))))
                    [open? g-score f-score came-from]))]
            (recur open? closed? g-score f-score came-from)))))))


