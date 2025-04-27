import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.nio.charset.StandardCharsets;

public class Lab1 {     //实现文本处理与图分析
    // 邻接表存储有向图
    private static final Map<String, Map<String, Integer>> graph = new HashMap<>();
    private static final Random random = new Random();

    public static void main(String[] args) throws IOException {
        String path = chooseFilePath();
        if (path == null) {
            System.out.println("未选择文件，程序退出。");
            return;
        }
        parseTextToGraph(path);     //解析文本构建图结构

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\n请选择功能：\n1. 展示图\n2. 查询桥接词\n3. 生成新文本\n4. 最短路径\n5. PageRank\n6. 随机游走\n7. 退出");
            switch (scanner.nextLine()) {
                case "1" :
                {
                    showDirectedGraph(graph);   // 生成并展示有向图
                    break;
                }
                case "2" :
                {   // 查询桥接词
                    System.out.print("输入word1: ");
                    String w1 = scanner.nextLine().toLowerCase();
                    System.out.print("输入word2: ");
                    String w2 = scanner.nextLine().toLowerCase();
                    System.out.println(queryBridgeWords(w1, w2));
                    break;
                }
                case "3" :
                {
                    System.out.print("输入新文本：");
                    System.out.println(generateNewText(scanner.nextLine()));
                    break;
                }
                case "4" :
                {
                    System.out.print("输入起点：");
                    String w1 = scanner.nextLine().toLowerCase();
                    System.out.print("输入终点：");
                    String w2 = scanner.nextLine().toLowerCase();
                    System.out.println(calcShortestPath(w1, w2));
                    break;
                }
                case "5" :
                {
                    System.out.print("输入阻尼因子（如 0.85）：");
                    try {
                        double d = Double.parseDouble(scanner.nextLine());
                        if (d < 0 || d > 1) throw new NumberFormatException();
                        Map<String, Double> pr = calPageRank(d);
                        System.out.println("所有单词的 PageRank 值：");
                        pr.entrySet().stream()
                                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                                .forEach(e -> System.out.printf("%-15s : %.6f\n", e.getKey(), e.getValue()));
                    } catch (NumberFormatException e) {
                        System.out.println("阻尼因子必须是 0 到 1 之间的小数！");
                    }
                    break;
                }
                case "6" :
                {
                    System.out.println("随机游走路径：" + randomWalk());
                    break;
                }
                case "7" :
                {
                    System.out.println("程序退出。");
                    return;
                }
                default :
                    System.out.println("无效输入。");
            }
        }
    }

    public static String chooseFilePath() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择文本文件");
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile().getAbsolutePath();
        }
        return null;
    }

    static void parseTextToGraph(String filePath) throws IOException {
        // 读取文件内容并处理为小写，移除非字母字符
        Path path = Paths.get(filePath);
        byte[] fileBytes = Files.readAllBytes(path);
        // 转换字节为字符串并处理
        String content = new String(fileBytes, StandardCharsets.UTF_8)
                .replaceAll("[^a-zA-Z\\s]", " ")  // 移除非字母字符
                .toLowerCase(); //转为小写
        String[] words = content.trim().split("\\s+");  //分割成单词数组
        // 遍历单词数组构建邻接表
        for (int i = 0; i < words.length - 1; i++) {
            graph.putIfAbsent(words[i], new HashMap<>());
            Map<String, Integer> edges = graph.get(words[i]);
            // 更新边权，即出现次数
            edges.put(words[i + 1], edges.getOrDefault(words[i + 1], 0) + 1);
        }
        // 确保最后一个词也加入图中，防止只作为终点却无法识别
        if (words.length > 0) {
            graph.putIfAbsent(words[words.length - 1], new HashMap<>());
        }
    }

    public static void showDirectedGraph(Map<String, Map<String, Integer>> G) {
        // 生成DOT语言描述的图结构
        StringBuilder dot = new StringBuilder("digraph G {\n");
        for (String from : G.keySet()) {
            for (var to : G.get(from).entrySet()) {
                dot.append(String.format("    \"%s\" -> \"%s\" [label=\"%d\"];\n", from, to.getKey(), to.getValue()));
            }
        }
        dot.append("}");

        try {
            // 写入DOT文件
            Files.writeString(Path.of("graph.dot"), dot.toString());
            System.out.println("DOT 文件已生成：graph.dot");
            // 调用Graphviz生成PNG图像
            ProcessBuilder pb = new ProcessBuilder("dot", "-Tpng", "graph.dot", "-o", "graph.png");
            pb.inheritIO();
            Process process = pb.start();
            process.waitFor();
            System.out.println("图已生成为 graph.png，即将打开！");
            Desktop.getDesktop().open(new File("graph.png"));
        } catch (IOException | InterruptedException e) {
            System.out.println("生成图失败：" + e.getMessage());
        }
    }

    private static boolean inGraph(String word) {
        // 检查词是否存在
        if (graph.containsKey(word)) return true;
        for (Map<String, Integer> edges : graph.values()) {
            if (edges.containsKey(word)) return true;
        }
        return false;
    }

    public static String queryBridgeWords(String word1, String word2) {
        // 查询桥接词
        // 检查词是否存在
        boolean hasWord1 = inGraph(word1);
        boolean hasWord2 = inGraph(word2);

        if (!hasWord1 && !hasWord2) {
            return "No \"" + word1 + "\" and \"" + word2 + "\" in the graph!";
        }
        if (!hasWord1) return "No \"" + word1 + "\" in the graph!";
        if (!hasWord2) return "No \"" + word2 + "\" in the graph!";

        if (!graph.containsKey(word1)) {    // world1无出边
            return "No bridge words from \"" + word1 + "\" to \"" + word2 + "\"!";
        }

        Set<String> bridges = new HashSet<>();  //查找world1与world2中间词
        for (String mid : graph.get(word1).keySet()) {
            if (graph.containsKey(mid) && graph.get(mid).containsKey(word2)) {
                bridges.add(mid);
            }
        }

        if (bridges.isEmpty()) {
            return "No bridge words from \"" + word1 + "\" to \"" + word2 + "\"!";
        }

        List<String> bridgeList = new ArrayList<>(bridges);
        String result;
        if (bridgeList.size() == 1) {
            result = bridgeList.get(0);
        } else if (bridgeList.size() == 2) {
            result = bridgeList.get(0) + " and " + bridgeList.get(1);
        } else {
            result = String.join(", ", bridgeList.subList(0, bridgeList.size() - 1))
                    + ", and " + bridgeList.get(bridgeList.size() - 1);
        }

        return "The bridge words from \"" + word1 + "\" to \"" + word2 + "\" are: " + result + ".";
    }

    public static String generateNewText(String inputText) {
        // 根据桥接词生成新文本
        // 预处理输入文本
        String[] words = inputText.toLowerCase().replaceAll("[^a-zA-Z\\s]", " ").split("\\s+");
        StringBuilder result = new StringBuilder(words[0]);
        for (int i = 0; i < words.length - 1; i++) {
            String w1 = words[i], w2 = words[i + 1];
            Set<String> bridges = new HashSet<>();
            // 查询桥接词
            if (graph.containsKey(w1)) {
                for (String mid : graph.get(w1).keySet()) {
                    if (graph.containsKey(mid) && graph.get(mid).containsKey(w2)) bridges.add(mid);
                }
            }
            // 随机插入一个桥接词
            if (!bridges.isEmpty()) {
                List<String> bridgeList = new ArrayList<>(bridges);
                String bridge = bridgeList.get(random.nextInt(bridgeList.size()));
                result.append(" ").append(bridge);
            }
            result.append(" ").append(w2);
        }
        return result.toString();
    }

    public static String calcShortestPath(String word1, String word2) {
        // 计算两词之间最短路径（Dijkstra算法）、
        // 判断word1，word2是否在图中
        if (!graph.containsKey(word1)) return "No \"" + word1 + "\" in the graph!";
        if (!graph.containsKey(word2)) return "No \"" + word2 + "\" in the graph!";
        Map<String, Integer> dist = new HashMap<>();    // 记录起点到各节点距离
        Map<String, String> prev = new HashMap<>();     // 前驱节点
        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingInt(dist::get));
        for (String node : graph.keySet()) dist.put(node, Integer.MAX_VALUE);   // 距离初始化为无穷大
        dist.put(word1, 0);
        pq.add(word1);

        while (!pq.isEmpty()) {     // Dijkstra算法
            String u = pq.poll();   // 取出当前距离最短节点
            if (!graph.containsKey(u)) continue;    // 若无出边，略过
            for (var e : graph.get(u).entrySet()) {     // 更新边权
                String v = e.getKey();
                int weight = e.getValue();
                if (dist.get(u) + weight < dist.getOrDefault(v, Integer.MAX_VALUE)) {
                    dist.put(v, dist.get(u) + weight);
                    prev.put(v, u);
                    pq.add(v);
                }
            }
        }
        // 路径不存在
        if (!dist.containsKey(word2) || dist.get(word2) == Integer.MAX_VALUE)
            return "No path from \"" + word1 + "\" to \"" + word2 + "\"";
        // 打印路径与最短距离
        LinkedList<String> path = new LinkedList<>();
        for (String at = word2; at != null; at = prev.get(at)) path.addFirst(at);
        return "Shortest path: " + String.join(" -> ", path) + "\nLength: " + dist.get(word2);
    }

    public static Map<String, Double> calPageRank(double dampingFactor) {
        // 计算每个结点PageRank
        final int maxIter = 100;
        final double tol = 1e-6;
        Set<String> nodes = graph.keySet();
        Map<String, Double> pr = new HashMap<>();
        for (String n : nodes) pr.put(n, 1.0 / nodes.size());

        for (int it = 0; it < maxIter; it++) {
            Map<String, Double> next = new HashMap<>();
            for (String n : nodes) next.put(n, (1 - dampingFactor) / nodes.size());

            for (String u : graph.keySet()) {
                Map<String, Integer> out = graph.get(u);
                if (out.isEmpty()) continue;
                double share = pr.get(u) / out.size();
                for (String v : out.keySet()) {
                    next.put(v, next.getOrDefault(v, 0.0) + dampingFactor * share);
                }
            }
            // 判断是否收敛
            double delta = 0.0;
            for (String n : nodes)
                delta += Math.abs(next.get(n) - pr.get(n));
            pr = next;
            if (delta < tol) break;
        }
        return pr;
    }

    public static String randomWalk() {
        // 随机游走
        StringBuilder sb = new StringBuilder();
        Set<String> visitedEdges = new HashSet<>();
        List<String> keys = new ArrayList<>(graph.keySet());
        if (keys.isEmpty()) return "";
        // 随机选择起点
        String current = keys.get(random.nextInt(keys.size()));
        sb.append(current);
        while (graph.containsKey(current) && !graph.get(current).isEmpty()) {
            List<String> nextNodes = new ArrayList<>(graph.get(current).keySet());
            String next = nextNodes.get(random.nextInt(nextNodes.size()));
            String edge = current + "->" + next;
            // 遇到重复边则停止
            if (visitedEdges.contains(edge)) break;
            visitedEdges.add(edge);
            sb.append(" ").append(next);
            current = next;
        }
        try {
            Files.writeString(Path.of("random_walk.txt"), sb.toString());
        } catch (IOException ignored) {}
        return sb.toString();
    }
}