const state = {
    locale: null,
    health: null,
    lastAction: null,
    lastGraphId: null,
    businessResponse: null,
    slowSql: [],
    exceptionContext: [],
    probes: [],
    probeValues: {},
    projectTools: [],
    toolResult: null,
    executionGraph: null,
    error: null
};

const endpoints = {
    loadOrder: "/orders/1",
    slowOrder: "/orders/slow",
    failOrder: "/orders/fail",
    probeOrder: "/orders/probe/1",
    slowSql: "/internal/spring-lens/slow-sql?limit=5&minDurationMs=1",
    exceptionContext: "/internal/spring-lens/exception-context?limit=5",
    probes: "/internal/spring-lens/probes",
    watchValues: "/internal/spring-lens/probe-values?probeId=order.lookup.result&limit=5",
    manualStatusValues: "/internal/spring-lens/probe-values?probeId=order.status&limit=5",
    manualCustomerValues: "/internal/spring-lens/probe-values?probeId=order.customer_name&limit=5",
    manualSummaryValues: "/internal/spring-lens/probe-values?probeId=order.summary&limit=5",
    projectTools: "/internal/spring-lens/project-tools",
    projectToolInvocation: "/internal/spring-lens/project-tools/count_orders_by_status:invoke"
};

const translations = {
    en: {
        languageName: "EN",
        pageTitle: "Spring Lens Runtime Console",
        eyebrow: "Spring Lens Demo Runtime Console",
        heroTitle: "AI Runtime Operating System",
        heroText: "This page is a live runtime lab for the demo app. It lets you trigger the implemented Spring Lens features, inspect structured runtime facts, and understand which AI-facing MCP capabilities those facts enable.",
        heroPills: ["Execution Graph", "Slow SQL", "Exception Context", "Programmable Probes", "Project Tools"],
        liveDemoContext: "Live Demo Context",
        runtimeBase: "Runtime Base",
        lastAction: "Last Action",
        lastGraph: "Last Graph",
        health: "Health",
        checking: "Checking",
        none: "None",
        sectionImplemented: "What Is Already Working",
        implementedTitle: "Implemented Runtime Capabilities",
        capabilityRuntime: "Runtime Observability",
        capabilityRuntimeText: "Captures HTTP request context, JDBC slow SQL, and exception events as structured runtime facts.",
        capabilityGraph: "Execution Graph",
        capabilityGraphText: "Builds a graph of runtime nodes and edges so AI systems can correlate cause, path, and outcome.",
        capabilityProbe: "Programmable Probes",
        capabilityProbeText: "Supports @LensWatch and Lens.look(...) to expose business-level runtime values.",
        capabilityTool: "Project Tools",
        capabilityToolText: "Supports @LensTool so application code can expose callable diagnostic functions.",
        sectionAi: "What AI Can Use",
        aiTitle: "MCP-Facing Capability Map",
        slowSqlPath: "Slow SQL Path",
        exceptionPath: "Exception Path",
        probePath: "Probe Path",
        projectToolPath: "Project Tool Path",
        pageAction: "Page action:",
        aiTools: "AI tools:",
        triggerSlowSql: "Trigger Slow SQL",
        triggerException: "Trigger Exception",
        triggerProbe: "Trigger Probe",
        invokeProjectTool: "Invoke Project Tool",
        sectionInteractive: "Interactive Console",
        interactiveTitle: "Run Runtime Experiments",
        refreshHealth: "Refresh Health",
        loadOrder: "Load Order",
        loadOrderDesc: "Call GET /orders/1 and show the baseline business response.",
        triggerSlowSqlDesc: "Execute the demo slow SQL path and pull the resulting SQL record plus graph.",
        triggerExceptionDesc: "Deliberately hit the failing path and inspect captured exception context.",
        triggerProbeDesc: "Run the demo probe flow and inspect @LensWatch plus manual probe values.",
        invokeProjectToolDesc: "List project tools and invoke count_orders_by_status with PAID.",
        noAction: "No action has been run yet.",
        sectionResult: "Result Panel",
        businessResponse: "Business Response",
        slowSql: "Slow SQL",
        exceptionContext: "Exception Context",
        probeValues: "Probe Values",
        projectTool: "Project Tool",
        emptyBusiness: "Run an action to populate this panel.",
        emptySlowSql: "No slow SQL result yet.",
        emptyException: "No exception context yet.",
        emptyProbe: "No probe data yet.",
        emptyTool: "No project tool invocation yet.",
        sectionGraph: "Execution Graph",
        graphTitle: "Latest Runtime Graph",
        emptyGraph: "Run a runtime-producing action to populate the graph.",
        reachable: "Reachable",
        unavailable: "Unavailable",
        probesWord: "probes",
        toolsWord: "tools",
        running: "Running",
        completed: "completed.",
        failed: "failed.",
        statusLoadOrder: "Load Order",
        statusSlowSql: "Trigger Slow SQL",
        statusException: "Trigger Exception",
        statusProbe: "Trigger Probe",
        statusProjectTool: "Invoke Project Tool",
        registeredProbes: "Registered Probes",
        watchValues: "@LensWatch Values",
        manualValues: "Lens.look(...) Values",
        discoveredProjectTools: "Discovered Project Tools",
        invocationResult: "Invocation Result",
        context: "Context",
        nodes: "Nodes",
        edges: "Edges",
        rawGraph: "Raw Execution Graph JSON",
        relation: "Relation",
        source: "Source",
        target: "Target",
        requestPath: "Request Path",
        occurredAt: "Occurred At",
        graph: "Graph",
        message: "Message",
        stackTrace: "Stack Trace",
        name: "Name",
        nodeId: "Node Id",
        sql: "SQL"
    },
    zh: {
        languageName: "中文",
        pageTitle: "Spring Lens 运行时控制台",
        eyebrow: "Spring Lens Demo 运行时控制台",
        heroTitle: "AI Runtime Operating System",
        heroText: "这个页面是 demo 应用的运行时实验台。你可以直接触发 Spring Lens 已实现的能力，查看结构化运行时事实，并理解这些事实最终会给 AI 提供哪些 MCP 能力。",
        heroPills: ["执行图", "慢 SQL", "异常上下文", "可编程探针", "项目工具"],
        liveDemoContext: "当前演示上下文",
        runtimeBase: "运行时地址",
        lastAction: "最近动作",
        lastGraph: "最近图 ID",
        health: "健康状态",
        checking: "检测中",
        none: "无",
        sectionImplemented: "当前已经完成了什么",
        implementedTitle: "已实现的运行时能力",
        capabilityRuntime: "运行时观测",
        capabilityRuntimeText: "把 HTTP 请求上下文、JDBC 慢 SQL 和异常事件采集为结构化运行时事实。",
        capabilityGraph: "Execution Graph",
        capabilityGraphText: "构建带节点和边的运行时图，让 AI 可以关联因果、路径和结果。",
        capabilityProbe: "可编程探针",
        capabilityProbeText: "支持 @LensWatch 和 Lens.look(...)，把业务级运行时值暴露出来。",
        capabilityTool: "项目工具",
        capabilityToolText: "支持 @LensTool，让应用代码声明可调用的诊断函数。",
        sectionAi: "给 AI 的能力映射",
        aiTitle: "MCP 能力映射",
        slowSqlPath: "慢 SQL 路径",
        exceptionPath: "异常路径",
        probePath: "探针路径",
        projectToolPath: "项目工具路径",
        pageAction: "页面动作：",
        aiTools: "AI 工具：",
        triggerSlowSql: "触发慢 SQL",
        triggerException: "触发异常",
        triggerProbe: "触发探针",
        invokeProjectTool: "调用项目工具",
        sectionInteractive: "交互控制台",
        interactiveTitle: "运行实验",
        refreshHealth: "刷新状态",
        loadOrder: "加载订单",
        loadOrderDesc: "调用 GET /orders/1，展示最基础的业务返回。",
        triggerSlowSqlDesc: "执行 demo 中的慢 SQL 路径，并拉取对应 SQL 记录和执行图。",
        triggerExceptionDesc: "故意触发失败路径，并查看捕获到的异常上下文。",
        triggerProbeDesc: "运行 demo 探针流程，查看 @LensWatch 和手动探针值。",
        invokeProjectToolDesc: "列出项目工具，并以 PAID 作为参数调用 count_orders_by_status。",
        noAction: "尚未执行任何动作。",
        sectionResult: "结果面板",
        businessResponse: "业务返回",
        slowSql: "慢 SQL",
        exceptionContext: "异常上下文",
        probeValues: "探针值",
        projectTool: "项目工具",
        emptyBusiness: "执行动作后这里会显示结果。",
        emptySlowSql: "还没有慢 SQL 结果。",
        emptyException: "还没有异常上下文。",
        emptyProbe: "还没有探针数据。",
        emptyTool: "还没有项目工具调用结果。",
        sectionGraph: "执行图",
        graphTitle: "最新运行时图",
        emptyGraph: "执行一个会产生运行时数据的动作后，这里会显示对应执行图。",
        reachable: "可达",
        unavailable: "不可达",
        probesWord: "个探针",
        toolsWord: "个工具",
        running: "正在执行",
        completed: "已完成。",
        failed: "失败。",
        statusLoadOrder: "加载订单",
        statusSlowSql: "触发慢 SQL",
        statusException: "触发异常",
        statusProbe: "触发探针",
        statusProjectTool: "调用项目工具",
        registeredProbes: "已注册探针",
        watchValues: "@LensWatch 捕获值",
        manualValues: "Lens.look(...) 捕获值",
        discoveredProjectTools: "已发现项目工具",
        invocationResult: "调用结果",
        context: "上下文",
        nodes: "节点",
        edges: "边",
        rawGraph: "原始 Execution Graph JSON",
        relation: "关系",
        source: "源节点",
        target: "目标节点",
        requestPath: "请求路径",
        occurredAt: "发生时间",
        graph: "图 ID",
        message: "消息",
        stackTrace: "堆栈",
        name: "名称",
        nodeId: "节点 ID",
        sql: "SQL"
    }
};

function preferredLocale() {
    const saved = window.localStorage.getItem("spring-lens-console-locale");
    if (saved === "zh" || saved === "en") {
        return saved;
    }
    return navigator.language && navigator.language.toLowerCase().startsWith("zh") ? "zh" : "en";
}

function t(key) {
    return translations[state.locale][key] ?? translations.en[key] ?? key;
}

document.addEventListener("DOMContentLoaded", () => {
    state.locale = preferredLocale();
    renderStaticShell();
    document.getElementById("runtime-base").textContent = window.location.origin;
    document.querySelectorAll("[data-action]").forEach((button) => {
        button.addEventListener("click", () => runAction(button.dataset.action));
    });
    document.getElementById("refresh-health").addEventListener("click", refreshHealth);
    document.getElementById("locale-switch").addEventListener("click", toggleLocale);
    refreshHealth();
    renderAll();
});

async function runAction(action) {
    setStatus(`${t("running")} ${labelForAction(action)}...`, "neutral");
    clearError();

    try {
        if (action === "load-order") {
            state.businessResponse = await getJson(endpoints.loadOrder);
            state.lastAction = t("statusLoadOrder");
        }
        else if (action === "slow-sql") {
            state.businessResponse = await getJson(endpoints.slowOrder);
            state.slowSql = await getJson(endpoints.slowSql);
            state.lastAction = t("statusSlowSql");
            await pullLatestGraphFrom(state.slowSql);
        }
        else if (action === "exception") {
            state.businessResponse = await getErrorJson(endpoints.failOrder);
            state.exceptionContext = await getJson(endpoints.exceptionContext);
            state.lastAction = t("statusException");
            await pullLatestGraphFrom(state.exceptionContext);
        }
        else if (action === "probe") {
            state.businessResponse = await getJson(endpoints.probeOrder);
            state.probes = await getJson(endpoints.probes);
            const manualGroups = await Promise.all([
                getJson(endpoints.manualStatusValues),
                getJson(endpoints.manualCustomerValues),
                getJson(endpoints.manualSummaryValues)
            ]);
            state.probeValues = {
                watch: await getJson(endpoints.watchValues),
                manual: manualGroups.flat()
            };
            state.lastAction = t("statusProbe");
            await pullLatestGraphFrom(state.probeValues.watch, state.probeValues.manual);
        }
        else if (action === "project-tool") {
            state.projectTools = await getJson(endpoints.projectTools);
            state.toolResult = await postJson(endpoints.projectToolInvocation, {
                arguments: {
                    status: "PAID"
                }
            });
            state.lastAction = t("statusProjectTool");
        }

        setStatus(`${state.lastAction} ${t("completed")}`, "success");
        renderAll();
        await refreshHealth();
    }
    catch (error) {
        state.error = error.message;
        setStatus(`${labelForAction(action)} ${t("failed")}`, "error");
        renderAll();
    }
}

async function refreshHealth() {
    try {
        const [probes, tools] = await Promise.all([
            getJson(endpoints.probes),
            getJson(endpoints.projectTools)
        ]);
        state.health = {
            reachable: true,
            probeCount: probes.length,
            projectToolCount: tools.length,
            checkedAt: new Date().toISOString()
        };
        renderHeader();
    }
    catch (error) {
        state.health = {
            reachable: false,
            message: error.message,
            checkedAt: new Date().toISOString()
        };
        renderHeader();
    }
}

async function pullLatestGraphFrom(...collections) {
    const latest = collections.flat().find((item) => item && item.graphId);
    if (!latest) {
        return;
    }
    state.lastGraphId = latest.graphId;
    state.executionGraph = await getJson(`/internal/spring-lens/graphs/${latest.graphId}`);
}

async function getJson(url) {
    const response = await fetch(url, { headers: { Accept: "application/json" } });
    if (!response.ok) {
        const text = await response.text();
        throw new Error(`${response.status} ${response.statusText}: ${text}`);
    }
    return response.json();
}

async function getErrorJson(url) {
    const response = await fetch(url, { headers: { Accept: "application/json" } });
    const text = await response.text();
    if (!text) {
        return { status: response.status };
    }
    try {
        return JSON.parse(text);
    }
    catch (_) {
        return { status: response.status, body: text };
    }
}

async function postJson(url, body) {
    const response = await fetch(url, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            Accept: "application/json"
        },
        body: JSON.stringify(body)
    });
    if (!response.ok) {
        const text = await response.text();
        throw new Error(`${response.status} ${response.statusText}: ${text}`);
    }
    return response.json();
}

function toggleLocale() {
    state.locale = state.locale === "zh" ? "en" : "zh";
    window.localStorage.setItem("spring-lens-console-locale", state.locale);
    renderStaticShell();
    renderAll();
}

function renderStaticShell() {
    document.documentElement.lang = state.locale;
    document.title = t("pageTitle");
    document.querySelector(".eyebrow").textContent = t("eyebrow");
    document.querySelector(".hero-copy h1").textContent = t("heroTitle");
    document.querySelector(".hero-text").textContent = t("heroText");
    document.querySelector(".hero-panel-label").textContent = t("liveDemoContext");
    document.querySelectorAll(".hero-pills .pill").forEach((pill, index) => {
        pill.textContent = t("heroPills")[index];
    });

    const dtNodes = document.querySelectorAll(".stat-list dt");
    dtNodes[0].textContent = t("runtimeBase");
    dtNodes[1].textContent = t("lastAction");
    dtNodes[2].textContent = t("lastGraph");
    dtNodes[3].textContent = t("health");

    document.getElementById("refresh-health").textContent = t("refreshHealth");

    const cards = document.querySelectorAll(".card");
    cards[0].querySelector(".section-kicker").textContent = t("sectionImplemented");
    cards[0].querySelector("h2").textContent = t("implementedTitle");
    cards[0].querySelectorAll("h3")[0].textContent = t("capabilityRuntime");
    cards[0].querySelectorAll("h3")[1].textContent = t("capabilityGraph");
    cards[0].querySelectorAll("h3")[2].textContent = t("capabilityProbe");
    cards[0].querySelectorAll("h3")[3].textContent = t("capabilityTool");
    cards[0].querySelectorAll("p")[1].textContent = t("capabilityRuntimeText");
    cards[0].querySelectorAll("p")[2].textContent = t("capabilityGraphText");
    cards[0].querySelectorAll("p")[3].innerHTML = `${escapeHtml(t("capabilityProbeText")).replace("@LensWatch", "<code>@LensWatch</code>").replace("Lens.look(...)", "<code>Lens.look(...)</code>")}`;
    cards[0].querySelectorAll("p")[4].innerHTML = `${escapeHtml(t("capabilityToolText")).replace("@LensTool", "<code>@LensTool</code>")}`;

    cards[1].querySelector(".section-kicker").textContent = t("sectionAi");
    cards[1].querySelector("h2").textContent = t("aiTitle");
    const mappingTitles = [t("slowSqlPath"), t("exceptionPath"), t("probePath"), t("projectToolPath")];
    cards[1].querySelectorAll("h3").forEach((node, index) => { node.textContent = mappingTitles[index]; });
    const mappingPs = cards[1].querySelectorAll("p");
    mappingPs[0].innerHTML = `${t("pageAction")} <strong>${t("triggerSlowSql")}</strong>`;
    mappingPs[1].innerHTML = `${t("aiTools")} <code>get_slow_sql</code>, <code>get_execution_graph</code>`;
    mappingPs[2].innerHTML = `${t("pageAction")} <strong>${t("triggerException")}</strong>`;
    mappingPs[3].innerHTML = `${t("aiTools")} <code>get_exception_context</code>, <code>get_diagnostic_playbook</code>`;
    mappingPs[4].innerHTML = `${t("pageAction")} <strong>${t("triggerProbe")}</strong>`;
    mappingPs[5].innerHTML = `${t("aiTools")} <code>query_probe_values</code>, <code>get_execution_graph</code>`;
    mappingPs[6].innerHTML = `${t("pageAction")} <strong>${t("invokeProjectTool")}</strong>`;
    mappingPs[7].innerHTML = `${t("aiTools")} <code>list_project_tools</code>, <code>invoke_project_tool</code>`;

    cards[2].querySelector(".section-kicker").textContent = t("sectionInteractive");
    cards[2].querySelector("h2").textContent = t("interactiveTitle");
    const actionTitles = [t("loadOrder"), t("triggerSlowSql"), t("triggerException"), t("triggerProbe"), t("invokeProjectTool")];
    const actionDescs = [t("loadOrderDesc"), t("triggerSlowSqlDesc"), t("triggerExceptionDesc"), t("triggerProbeDesc"), t("invokeProjectToolDesc")];
    document.querySelectorAll(".action-title").forEach((node, index) => { node.textContent = actionTitles[index]; });
    document.querySelectorAll(".action-desc").forEach((node, index) => { node.innerHTML = actionDescs[index].replace("GET /orders/1", "<code>GET /orders/1</code>").replace("count_orders_by_status", "<code>count_orders_by_status</code>").replace("PAID", "<code>PAID</code>").replace("@LensWatch", "<code>@LensWatch</code>"); });

    cards[3].querySelector(".section-kicker").textContent = t("sectionResult");
    cards[3].querySelector("h2").textContent = t("businessResponse");
    cards[4].querySelector(".section-kicker").textContent = t("sectionResult");
    cards[4].querySelector("h2").textContent = t("slowSql");
    cards[5].querySelector(".section-kicker").textContent = t("sectionResult");
    cards[5].querySelector("h2").textContent = t("exceptionContext");
    cards[6].querySelector(".section-kicker").textContent = t("sectionResult");
    cards[6].querySelector("h2").textContent = t("probeValues");
    cards[7].querySelector(".section-kicker").textContent = t("sectionResult");
    cards[7].querySelector("h2").textContent = t("projectTool");
    cards[8].querySelector(".section-kicker").textContent = t("sectionGraph");
    cards[8].querySelector("h2").textContent = t("graphTitle");

    document.getElementById("locale-switch")?.remove();
    const localeButton = document.createElement("button");
    localeButton.id = "locale-switch";
    localeButton.className = "secondary-button";
    localeButton.textContent = state.locale === "zh" ? "EN" : "中文";
    document.querySelector(".hero-panel").prepend(localeButton);
    localeButton.addEventListener("click", toggleLocale);
}

function renderAll() {
    renderHeader();
    renderError();
    renderBusinessResponse();
    renderSlowSql();
    renderExceptionContext();
    renderProbes();
    renderProjectTools();
    renderGraph();
}

function renderHeader() {
    document.getElementById("last-action").textContent = state.lastAction || t("none");
    document.getElementById("last-graph-id").textContent = state.lastGraphId || t("none");
    const badge = document.getElementById("health-badge");
    if (!state.health) {
        badge.innerHTML = `<span class="pill pill-muted">${t("checking")}</span>`;
        return;
    }
    if (state.health.reachable) {
        const probeText = state.locale === "zh"
            ? `${state.health.probeCount}${t("probesWord")}`
            : `${state.health.probeCount} ${t("probesWord")}`;
        const toolText = state.locale === "zh"
            ? `${state.health.projectToolCount}${t("toolsWord")}`
            : `${state.health.projectToolCount} ${t("toolsWord")}`;
        badge.innerHTML = `<span class="pill pill-success">${t("reachable")} · ${probeText} · ${toolText}</span>`;
        return;
    }
    badge.innerHTML = `<span class="pill pill-danger">${t("unavailable")}</span>`;
}

function renderError() {
    const banner = document.getElementById("error-banner");
    if (!state.error) {
        banner.classList.add("hidden");
        banner.textContent = "";
        return;
    }
    banner.classList.remove("hidden");
    banner.textContent = state.error;
}

function renderBusinessResponse() {
    document.getElementById("business-response").innerHTML = state.businessResponse
        ? jsonBlock(state.businessResponse)
        : empty(t("emptyBusiness"));
}

function renderSlowSql() {
    const container = document.getElementById("slow-sql-result");
    if (!state.slowSql.length) {
        container.innerHTML = empty(t("emptySlowSql"));
        return;
    }
    container.innerHTML = listStack(state.slowSql.map((record) => `
        <article class="node-card">
            <div class="status-row">
                <span class="pill pill-accent">${escapeHtml(t("graph"))} ${escapeHtml(record.graphId)}</span>
                <span class="pill pill-success">${escapeHtml(String(record.durationMs))} ms</span>
            </div>
            <div class="kv-grid">
                <div><dt>${escapeHtml(t("requestPath"))}</dt><dd>${escapeHtml(record.requestPath || "n/a")}</dd></div>
                <div><dt>${escapeHtml(t("sql"))}</dt><dd>${escapeHtml(record.sql)}</dd></div>
                <div><dt>${escapeHtml(t("occurredAt"))}</dt><dd>${escapeHtml(record.occurredAt)}</dd></div>
            </div>
        </article>
    `));
}

function renderExceptionContext() {
    const container = document.getElementById("exception-result");
    if (!state.exceptionContext.length) {
        container.innerHTML = empty(t("emptyException"));
        return;
    }
    container.innerHTML = listStack(state.exceptionContext.map((record) => `
        <article class="node-card">
            <div class="status-row">
                <span class="pill pill-danger">${escapeHtml(record.exceptionClass)}</span>
                <span class="pill pill-accent">${escapeHtml(record.requestPath || "n/a")}</span>
            </div>
            <div class="kv-grid">
                <div><dt>${escapeHtml(t("graph"))}</dt><dd>${escapeHtml(record.graphId)}</dd></div>
                <div><dt>${escapeHtml(t("message"))}</dt><dd>${escapeHtml(record.message || "(empty)")}</dd></div>
                <div><dt>${escapeHtml(t("occurredAt"))}</dt><dd>${escapeHtml(record.occurredAt)}</dd></div>
            </div>
            ${record.stackTrace?.length ? `<details><summary>${escapeHtml(t("stackTrace"))}</summary>${jsonBlock(record.stackTrace)}</details>` : ""}
        </article>
    `));
}

function renderProbes() {
    const container = document.getElementById("probe-result");
    const watchValues = state.probeValues.watch || [];
    const manualValues = state.probeValues.manual || [];
    if (!state.probes.length && !watchValues.length && !manualValues.length) {
        container.innerHTML = empty(t("emptyProbe"));
        return;
    }
    container.innerHTML = `
        <div class="list-stack">
            <article class="graph-card"><h3>${escapeHtml(t("registeredProbes"))}</h3>${state.probes.length ? jsonBlock(state.probes) : empty(t("emptyProbe"))}</article>
            <article class="graph-card"><h3>${escapeHtml(t("watchValues"))}</h3>${watchValues.length ? jsonBlock(watchValues) : empty(t("emptyProbe"))}</article>
            <article class="graph-card"><h3>${escapeHtml(t("manualValues"))}</h3>${manualValues.length ? jsonBlock(manualValues) : empty(t("emptyProbe"))}</article>
        </div>
    `;
}

function renderProjectTools() {
    const container = document.getElementById("tool-result");
    if (!state.projectTools.length && !state.toolResult) {
        container.innerHTML = empty(t("emptyTool"));
        return;
    }
    container.innerHTML = `
        <div class="list-stack">
            <article class="graph-card"><h3>${escapeHtml(t("discoveredProjectTools"))}</h3>${state.projectTools.length ? jsonBlock(state.projectTools) : empty(t("emptyTool"))}</article>
            <article class="graph-card"><h3>${escapeHtml(t("invocationResult"))}</h3>${state.toolResult ? jsonBlock(state.toolResult) : empty(t("emptyTool"))}</article>
        </div>
    `;
}

function renderGraph() {
    const container = document.getElementById("graph-summary");
    const graph = state.executionGraph;
    if (!graph) {
        container.innerHTML = empty(t("emptyGraph"));
        return;
    }
    const nodes = Array.isArray(graph.nodes) ? graph.nodes : [];
    const edges = Array.isArray(graph.edges) ? graph.edges : [];
    container.innerHTML = `
        <div class="graph-layout">
            <section class="graph-column"><article class="graph-card"><h3>${escapeHtml(t("context"))}</h3>${jsonBlock(graph.context)}</article></section>
            <section class="graph-column"><article class="graph-card"><h3>${escapeHtml(t("nodes"))} (${nodes.length})</h3><div class="list-stack">${nodes.map((node) => `
                <article class="node-card">
                    <div class="status-row">
                        <span class="pill pill-accent">${escapeHtml(node.type)}</span>
                        <span class="${statusPillClass(node.status)}">${escapeHtml(node.status)}</span>
                    </div>
                    <div class="kv-grid">
                        <div><dt>${escapeHtml(t("name"))}</dt><dd>${escapeHtml(node.name)}</dd></div>
                        <div><dt>${escapeHtml(t("nodeId"))}</dt><dd>${escapeHtml(node.nodeId)}</dd></div>
                    </div>
                </article>`).join("")}</div></article></section>
            <section class="graph-column"><article class="graph-card"><h3>${escapeHtml(t("edges"))} (${edges.length})</h3><div class="list-stack">${edges.map((edge) => `
                <article class="edge-card">
                    <div class="kv-grid">
                        <div><dt>${escapeHtml(t("relation"))}</dt><dd>${escapeHtml(edge.relation)}</dd></div>
                        <div><dt>${escapeHtml(t("source"))}</dt><dd>${escapeHtml(edge.sourceNodeId)}</dd></div>
                        <div><dt>${escapeHtml(t("target"))}</dt><dd>${escapeHtml(edge.targetNodeId)}</dd></div>
                    </div>
                </article>`).join("")}</div></article></section>
        </div>
        <details><summary>${escapeHtml(t("rawGraph"))}</summary>${jsonBlock(graph)}</details>
    `;
}

function setStatus(message, kind) {
    const status = document.getElementById("action-status");
    status.textContent = message;
    status.className = `feedback-chip ${kind === "success" ? "success" : kind === "error" ? "error" : "neutral"}`;
}

function clearError() {
    state.error = null;
}

function labelForAction(action) {
    if (action === "load-order") return t("statusLoadOrder");
    if (action === "slow-sql") return t("statusSlowSql");
    if (action === "exception") return t("statusException");
    if (action === "probe") return t("statusProbe");
    if (action === "project-tool") return t("statusProjectTool");
    return action;
}

function jsonBlock(value) {
    return `<pre class="json-block">${escapeHtml(JSON.stringify(value, null, 2))}</pre>`;
}

function listStack(items) {
    return `<div class="list-stack">${items.join("")}</div>`;
}

function empty(message) {
    return `<div class="empty-state">${escapeHtml(message)}</div>`;
}

function statusPillClass(status) {
    if (status === "SUCCESS") return "pill pill-success";
    if (status === "FAILURE") return "pill pill-danger";
    return "pill pill-muted";
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}
