var History = window.History;

function historyPush(url) {
	History.pushState(null, null, url);
}