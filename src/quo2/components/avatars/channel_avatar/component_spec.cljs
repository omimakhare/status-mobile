(ns quo2.components.avatars.channel-avatar.component-spec
  (:require [quo2.components.avatars.channel-avatar.view :as component]
            [test-helpers.component :as h]))

(h/describe "Channel Avatar"
  (h/test "default render"
    (h/render [component/view])
    (h/is-truthy (h/query-by-label-text :initials))
    (h/is-null (h/query-by-label-text :emoji))
    (h/is-null (h/query-by-role :channel-avatar-badge)))

  (h/test "with emoji, no badge, large size"
    (let [emoji "🍓"]
      (h/render [component/view {:emoji emoji :size :size-32}])
      (h/is-null (h/query-by-label-text :initials))
      (h/is-truthy (h/query-by-text emoji))
      (h/is-null (h/query-by-role :channel-avatar-badge))))

  (h/test "with locked badge"
    (h/render [component/view {:badge :locked}])
    (h/is-truthy (h/query-by-role :channel-avatar-badge-locked)))

  (h/test "with unlocked badge"
    (h/render [component/view {:badge :unlocked}])
    (h/is-truthy (h/query-by-label-text :channel-avatar-badge-unlocked)))

  (h/test "no emoji, smaller size"
    (h/render [component/view {:full-name "Status Mobile"}])
    (h/is-truthy (h/query-by-text "S")))

  (h/test "no emoji, large size"
    (h/render [component/view
               {:full-name "Status Mobile"
                :size      :size-32}])
    (h/is-truthy (h/query-by-text "SM"))))
