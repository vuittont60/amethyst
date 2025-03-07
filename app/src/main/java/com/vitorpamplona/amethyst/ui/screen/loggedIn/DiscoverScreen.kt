package com.vitorpamplona.amethyst.ui.screen.loggedIn

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.service.NostrDiscoveryDataSource
import com.vitorpamplona.amethyst.ui.navigation.Route
import com.vitorpamplona.amethyst.ui.note.ChannelCardCompose
import com.vitorpamplona.amethyst.ui.screen.FeedEmpty
import com.vitorpamplona.amethyst.ui.screen.FeedError
import com.vitorpamplona.amethyst.ui.screen.FeedState
import com.vitorpamplona.amethyst.ui.screen.FeedViewModel
import com.vitorpamplona.amethyst.ui.screen.LoadingFeed
import com.vitorpamplona.amethyst.ui.screen.NostrDiscoverChatFeedViewModel
import com.vitorpamplona.amethyst.ui.screen.NostrDiscoverCommunityFeedViewModel
import com.vitorpamplona.amethyst.ui.screen.NostrDiscoverLiveFeedViewModel
import com.vitorpamplona.amethyst.ui.screen.NostrDiscoverMarketplaceFeedViewModel
import com.vitorpamplona.amethyst.ui.screen.PagerStateKeys
import com.vitorpamplona.amethyst.ui.screen.RefresheableView
import com.vitorpamplona.amethyst.ui.screen.SaveableFeedState
import com.vitorpamplona.amethyst.ui.screen.SaveableGridFeedState
import com.vitorpamplona.amethyst.ui.screen.ScrollStateKeys
import com.vitorpamplona.amethyst.ui.screen.rememberForeverPagerState
import com.vitorpamplona.amethyst.ui.theme.FeedPadding
import com.vitorpamplona.amethyst.ui.theme.TabRowHeight
import com.vitorpamplona.quartz.events.ChannelCreateEvent
import com.vitorpamplona.quartz.events.ClassifiedsEvent
import com.vitorpamplona.quartz.events.CommunityDefinitionEvent
import com.vitorpamplona.quartz.events.LiveActivitiesEvent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DiscoverScreen(
    discoveryMarketplaceFeedViewModel: NostrDiscoverMarketplaceFeedViewModel,
    discoveryLiveFeedViewModel: NostrDiscoverLiveFeedViewModel,
    discoveryCommunityFeedViewModel: NostrDiscoverCommunityFeedViewModel,
    discoveryChatFeedViewModel: NostrDiscoverChatFeedViewModel,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    val lifeCycleOwner = LocalLifecycleOwner.current

    val tabs by remember(discoveryLiveFeedViewModel, discoveryCommunityFeedViewModel, discoveryChatFeedViewModel) {
        mutableStateOf(
            listOf(
                TabItem(R.string.discover_marketplace, discoveryMarketplaceFeedViewModel, Route.Discover.base + "Marketplace", ScrollStateKeys.DISCOVER_MARKETPLACE, ClassifiedsEvent.kind),
                TabItem(R.string.discover_live, discoveryLiveFeedViewModel, Route.Discover.base + "Live", ScrollStateKeys.DISCOVER_LIVE, LiveActivitiesEvent.kind),
                TabItem(R.string.discover_community, discoveryCommunityFeedViewModel, Route.Discover.base + "Community", ScrollStateKeys.DISCOVER_COMMUNITY, CommunityDefinitionEvent.kind),
                TabItem(R.string.discover_chat, discoveryChatFeedViewModel, Route.Discover.base + "Chats", ScrollStateKeys.DISCOVER_CHATS, ChannelCreateEvent.kind)
            ).toImmutableList()
        )
    }

    val pagerState = rememberForeverPagerState(key = PagerStateKeys.DISCOVER_SCREEN) { tabs.size }

    WatchAccountForDiscoveryScreen(
        discoverMarketplaceFeedViewModel = discoveryMarketplaceFeedViewModel,
        discoveryLiveFeedViewModel = discoveryLiveFeedViewModel,
        discoveryCommunityFeedViewModel = discoveryCommunityFeedViewModel,
        discoveryChatFeedViewModel = discoveryChatFeedViewModel,
        accountViewModel = accountViewModel
    )

    DisposableEffect(lifeCycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                println("Discovery Start")
                NostrDiscoveryDataSource.start()
            }
        }

        lifeCycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifeCycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(Modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier.padding(vertical = 0.dp)
        ) {
            DiscoverPages(pagerState, tabs, accountViewModel, nav)
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun DiscoverPages(
    pagerState: PagerState,
    tabs: ImmutableList<TabItem>,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    ScrollableTabRow(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        selectedTabIndex = pagerState.currentPage,
        modifier = TabRowHeight,
        edgePadding = 8.dp
    ) {
        val coroutineScope = rememberCoroutineScope()

        tabs.forEachIndexed { index, tab ->
            Tab(
                selected = pagerState.currentPage == index,
                text = {
                    Text(text = stringResource(tab.resource))
                },
                onClick = {
                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                }
            )
        }
    }

    HorizontalPager(state = pagerState) { page ->
        RefresheableView(tabs[page].viewModel, true) {
            if (tabs[page].viewModel is NostrDiscoverMarketplaceFeedViewModel) {
                SaveableGridFeedState(tabs[page].viewModel, scrollStateKey = tabs[page].scrollStateKey) { listState ->
                    RenderDiscoverFeed(
                        viewModel = tabs[page].viewModel,
                        routeForLastRead = tabs[page].routeForLastRead,
                        forceEventKind = tabs[page].forceEventKind,
                        listState = listState,
                        accountViewModel = accountViewModel,
                        nav = nav
                    )
                }
            } else {
                SaveableFeedState(tabs[page].viewModel, scrollStateKey = tabs[page].scrollStateKey) { listState ->
                    RenderDiscoverFeed(
                        viewModel = tabs[page].viewModel,
                        routeForLastRead = tabs[page].routeForLastRead,
                        forceEventKind = tabs[page].forceEventKind,
                        listState = listState,
                        accountViewModel = accountViewModel,
                        nav = nav
                    )
                }
            }
        }
    }
}

@Composable
private fun RenderDiscoverFeed(
    viewModel: FeedViewModel,
    routeForLastRead: String?,
    forceEventKind: Int?,
    listState: ScrollableState,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    val feedState by viewModel.feedContent.collectAsStateWithLifecycle()

    Crossfade(
        targetState = feedState,
        animationSpec = tween(durationMillis = 100),
        label = "RenderDiscoverFeed"
    ) { state ->
        when (state) {
            is FeedState.Empty -> {
                FeedEmpty {
                    viewModel.invalidateData()
                }
            }

            is FeedState.FeedError -> {
                FeedError(state.errorMessage) {
                    viewModel.invalidateData()
                }
            }

            is FeedState.Loaded -> {
                if (listState is LazyGridState) {
                    DiscoverFeedColumnsLoaded(
                        state,
                        routeForLastRead,
                        listState,
                        forceEventKind,
                        accountViewModel,
                        nav
                    )
                } else if (listState is LazyListState) {
                    DiscoverFeedLoaded(
                        state,
                        routeForLastRead,
                        listState,
                        forceEventKind,
                        accountViewModel,
                        nav
                    )
                }
            }

            is FeedState.Loading -> {
                LoadingFeed()
            }
        }
    }
}

@Composable
fun WatchAccountForDiscoveryScreen(
    discoverMarketplaceFeedViewModel: NostrDiscoverMarketplaceFeedViewModel,
    discoveryLiveFeedViewModel: NostrDiscoverLiveFeedViewModel,
    discoveryCommunityFeedViewModel: NostrDiscoverCommunityFeedViewModel,
    discoveryChatFeedViewModel: NostrDiscoverChatFeedViewModel,
    accountViewModel: AccountViewModel
) {
    val listState by accountViewModel.account.liveStoriesFollowLists.collectAsStateWithLifecycle()

    LaunchedEffect(accountViewModel, listState) {
        NostrDiscoveryDataSource.resetFilters()
        discoverMarketplaceFeedViewModel.checkKeysInvalidateDataAndSendToTop()
        discoveryLiveFeedViewModel.checkKeysInvalidateDataAndSendToTop()
        discoveryCommunityFeedViewModel.checkKeysInvalidateDataAndSendToTop()
        discoveryChatFeedViewModel.checkKeysInvalidateDataAndSendToTop()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiscoverFeedLoaded(
    state: FeedState.Loaded,
    routeForLastRead: String?,
    listState: LazyListState,
    forceEventKind: Int?,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    LazyColumn(
        contentPadding = FeedPadding,
        state = listState
    ) {
        itemsIndexed(state.feed.value, key = { _, item -> item.idHex }) { _, item ->
            val defaultModifier = remember {
                Modifier
                    .fillMaxWidth()
                    .animateItemPlacement()
            }

            Row(defaultModifier) {
                ChannelCardCompose(
                    baseNote = item,
                    routeForLastRead = routeForLastRead,
                    modifier = Modifier,
                    forceEventKind = forceEventKind,
                    accountViewModel = accountViewModel,
                    nav = nav
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiscoverFeedColumnsLoaded(
    state: FeedState.Loaded,
    routeForLastRead: String?,
    listState: LazyGridState,
    forceEventKind: Int?,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = FeedPadding,
        state = listState
    ) {
        itemsIndexed(state.feed.value, key = { _, item -> item.idHex }) { _, item ->
            val defaultModifier = remember {
                Modifier
                    .fillMaxWidth()
                    .animateItemPlacement()
            }

            Row(defaultModifier) {
                ChannelCardCompose(
                    baseNote = item,
                    routeForLastRead = routeForLastRead,
                    modifier = Modifier,
                    forceEventKind = forceEventKind,
                    accountViewModel = accountViewModel,
                    nav = nav
                )
            }
        }
    }
}
