import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import javax.swing.JFileChooser;
/**.
 * 实现文本处理与图分析功能的工具类。
 */

public class Lab1 {     //实现文本处理与图分析
  // 邻接表存储有向图
  private static final Map<String, Map<String, Integer>> graph = new HashMap<>();
  private static final Random random = new Random();
  /**.
   * 程序入口方法。通过图形界面选择文本文件，并解析该文件构建有向图结构。
   *
   * <p>程序首先调用 {@code chooseFilePath()} 方法弹出文件选择对话框，
   * 如果用户选择了文件，则调用 {@code parseTextToGraph(path)} 方法读取并解析文件内容，
   * 构建词语之间的有向图。如果用户取消选择，则程序输出提示信息并终止。</p>
   *
   * @param args 命令行参数（当前未使用）
   * @throws IOException 如果读取文件时发生 I/O 异常
   */
  
  public static void main(String[] args) throws IOException {
    String path = chooseFilePath();
    if (path == null) {
      System.out.println("未选择文件，程序退出。");
      return;
    }
    parseTextToGraph(path);     //解析文本构建图结构
    
    Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
    while (true) {
      System.out.println("""
    请选择功能：
    1. 展示图
    2. 查询桥接词
    3. 生成新文本
    4. 最短路径
    5. PageRank
    6. 随机游走
    7. 退出""");
      switch (scanner.nextLine()) {
        case "1":
          {
            showDirectedGraph(graph);   // 生成并展示有向图
            break;
          }
        case "2":
          {   // 查询桥接词
            System.out.print("输入word1: ");
            String w1 = scanner.nextLine().toLowerCase();
            System.out.print("输入word2: ");
            String w2 = scanner.nextLine().toLowerCase();
            System.out.println(queryBridgeWords(w1, w2));
            break;
          }
        case "3":
          {
            System.out.print("输入新文本：");
            System.out.println(generateNewText(scanner.nextLine()));
            break;
          }
        case "4":
          {
            System.out.print("输入起点：");
            String w1 = scanner.nextLine().toLowerCase();
            System.out.print("输入终点：");
            String w2 = scanner.nextLine().toLowerCase();
            System.out.println(calcShortestPath(w1, w2));
            break;
          }
        case "5":
          {
            System.out.print("输入阻尼因子（如 0.85）：");
            try {
              double d = Double.parseDouble(scanner.nextLine());
              if (d < 0 || d > 1) {
                throw new NumberFormatException();
              }
              Map<String, Double> pr = calPageRank(d);
              System.out.println("所有单词的 PageRank 值：");
              pr.entrySet().stream()
                  .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                  .forEach(e -> System.out.printf("%-15s : %.6f%n", e.getKey(), e.getValue()));
            } catch (NumberFormatException e) {
              System.out.println("阻尼因子必须是 0 到 1 之间的小数！");
            }
            break;
          }
        case "6":
          {
            System.out.println("随机游走路径：" + randomWalk());
            break;
          }
        case "7":
          {
            System.out.println("程序退出。");
            return;
          }
        default:
          System.out.println("无效输入。");
      }
    }
  }
  /**.
   * 弹出文件选择对话框，让用户选择一个文本文件。
   *
   * <p>该方法使用 {@link JFileChooser} 弹出一个文件选择器窗口，供用户选择本地文件。
   * 如果用户确认选择了一个文件，则返回该文件的绝对路径；
   * 如果用户取消选择，则返回 {@code null}。</p>
   *
   * @return 用户选择的文件的绝对路径，如果未选择则返回 {@code null}
   */
  
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
  /**.
   * 根据提供的有向图生成 DOT 文件，并调用 Graphviz 工具将其渲染为 PNG 图像。
   *
   * <p>该方法接收一个表示有向图的数据结构，其中每个节点映射到其所有出边及权重。
   * 它将图结构转换为 DOT 语言格式并写入文件 {@code graph.dot}，然后通过调用
   * 外部命令 {@code dot} 生成 {@code graph.png} 图像。成功生成后会尝试自动打开图像。</p>
   *
   * <p>此方法依赖本地已安装的 Graphviz 工具，并假设 {@code dot} 命令可在系统环境变量中访问。</p>
   *
   * @param g 表示有向图的邻接表，键为起始节点，值为终止节点及对应边权的映射
   */
  
  public static void showDirectedGraph(Map<String, Map<String, Integer>> g) {
    // 生成DOT语言描述的图结构
    StringBuilder dot = new StringBuilder("digraph g {\n");
    for (Map.Entry<String, Map<String, Integer>> fromEntry : g.entrySet()) {
      String from = fromEntry.getKey();
      Map<String, Integer> toMap = fromEntry.getValue();
      for (Map.Entry<String, Integer> toEntry : toMap.entrySet()) {
        dot.append(String.format("    \"%s\" -> \"%s\" [label=\"%d\"];%n",
            from, toEntry.getKey(), toEntry.getValue()));
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
    if (graph.containsKey(word)) {
      return true;
    }
    for (Map<String, Integer> edges : graph.values()) {
      if (edges.containsKey(word)) {
        return true;
      }
    }
    return false;
  }
  /**.
   * 查询在指定的两个单词之间存在的桥接词（Bridge Words）。
   *
   * <p>桥接词是指在有向图中存在一条从 {@code word1} 指向某个中间词，
   * 再从该中间词指向 {@code word2} 的路径。</p>
   *
   * <p>方法首先判断两个单词是否都存在于图中，并依次查找所有满足条件的桥接词。
   * 若不存在任何桥接词，将返回提示信息。</p>
   *
   * @param word1 起始单词
   * @param word2 终止单词
   * @return 表示桥接词结果的字符串说明；若单词不存在或无桥接词，也会返回相应提示信息
   */
  
  public static String queryBridgeWords(String word1, String word2) {
    // 查询桥接词
    // 检查词是否存在
    boolean hasWord1 = inGraph(word1);
    boolean hasWord2 = inGraph(word2);
    
    if (!hasWord1 && !hasWord2) {
      return "No \"" + word1 + "\" and \"" + word2 + "\" in the graph!";
    }
    if (!hasWord1) {
      return "No \"" + word1 + "\" in the graph!";
    }
    if (!hasWord2) {
      return "No \"" + word2 + "\" in the graph!";
    }
    
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
  /**.
   * 根据给定文本和图中桥接词生成新文本。
   *
   * <p>方法会对输入文本进行预处理（统一为小写、去除非字母字符），然后逐对单词检查是否存在桥接词。
   * 如果存在桥接词，则从候选中随机选取一个插入到两个单词之间，从而构造出新的文本内容。</p>
   *
   * <p>桥接词是指：若图中存在从 word1 到 mid，再从 mid 到 word2 的边，则 mid 是 word1 和 word2 的桥接词。</p>
   *
   * @param inputText 输入的原始文本（英文字符串）
   * @return 一个可能插入桥接词后的新文本字符串
   */
  
  public static String generateNewText(String inputText) {
    // 根据桥接词生成新文本
    // 预处理输入文本
    String[] words = inputText.toLowerCase().replaceAll("[^a-zA-Z\\s]", " ").split("\\s+");
    StringBuilder result = new StringBuilder(words[0]);
    for (int i = 0; i < words.length - 1; i++) {
      String w1 = words[i];
      String w2 = words[i + 1];
      Set<String> bridges = new HashSet<>();
      // 查询桥接词
      if (graph.containsKey(w1)) {
        for (String mid : graph.get(w1).keySet()) {
          if (graph.containsKey(mid) && graph.get(mid).containsKey(w2)) {
            bridges.add(mid);
          }
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
  /**.
   * 使用 Dijkstra 算法计算两个单词之间的最短路径。
   *
   * <p>该方法在图中查找从 {@code word1} 到 {@code word2} 的最短路径，
   * 并返回路径及其总权重（长度）。若其中任一单词不在图中，或两者之间不存在路径，
   * 将返回相应的提示信息。</p>
   *
   * @param word1 起始单词，作为路径起点
   * @param word2 目标单词，作为路径终点
   * @return 表示最短路径及其长度的字符串，若路径不存在或单词无效，则返回错误提示
   */
  
  public static String calcShortestPath(String word1, String word2) {
    // 计算两词之间最短路径（Dijkstra算法）、
    // 判断word1，word2是否在图中
    if (!graph.containsKey(word1)) {
      return "No \"" + word1 + "\" in the graph!";
    }
    if (!graph.containsKey(word2)) {
      return "No \"" + word2 + "\" in the graph!";
    }
    Map<String, Integer> dist = new HashMap<>();    // 记录起点到各节点距离
    PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingInt(dist::get));
    for (String node : graph.keySet()) {
      dist.put(node, Integer.MAX_VALUE);
    }   // 距离初始化为无穷大
    dist.put(word1, 0);
    pq.add(word1);
    Map<String, String> prev = new HashMap<>();     // 前驱节点
    while (!pq.isEmpty()) {     // Dijkstra算法
      String u = pq.poll();   // 取出当前距离最短节点
      if (!graph.containsKey(u)) {
        continue;
      }    // 若无出边，略过
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
    if (!dist.containsKey(word2) || dist.get(word2) == Integer.MAX_VALUE) {
      return "No path from \"" + word1 + "\" to \"" + word2 + "\"";
    }
    // 打印路径与最短距离
    LinkedList<String> path = new LinkedList<>();
    for (String at = word2; at != null; at = prev.get(at)) {
      path.addFirst(at);
    }
    return "Shortest path: " + String.join(" -> ", path) + "\nLength: " + dist.get(word2);
  }
  /**.
   * 计算图中每个节点的 PageRank 值。
   * 该方法使用迭代方式实现简化版的 PageRank 算法。
   * PageRank 反映了节点在图中的“重要性”或“影响力”，适用于有向图（如网络中的网页链接关系）。
   * 初始每个节点的 PageRank 相同，随后通过迭代，根据出边将权重分配给相邻节点。
   *
   * @param dampingFactor 阻尼因子（一般建议在 0.8~0.9 之间），用于控制随机跳转概率
   * @return 一个包含所有节点及其 PageRank 值的 Map
   */
  
  public static Map<String, Double> calPageRank(double dampingFactor) {
    // 计算每个结点PageRank
    final int maxIter = 100;
    final double tol = 1e-6;
    Set<String> nodes = graph.keySet();     //所有节点
    Map<String, Double> pr = new HashMap<>();   // 存储PageRank
    for (String n : nodes) {
      pr.put(n, 1.0 / nodes.size());
    }
    for (int it = 0; it < maxIter; it++) {
      Map<String, Double> next = new HashMap<>();
      for (String n : nodes) {
        next.put(n, (1 - dampingFactor) / nodes.size());
      }
      
      for (Map.Entry<String, Map<String, Integer>> graphEntry : graph.entrySet()) {
        String u = graphEntry.getKey();
        Map<String, Integer> out = graphEntry.getValue();  // u's outgoing edges
        
        double share = pr.get(u) / out.size();
        
        for (Map.Entry<String, Integer> outEntry : out.entrySet()) {
          String v = outEntry.getKey();
          next.put(v, next.getOrDefault(v, 0.0) + dampingFactor * share);
        }
      }
      
      // 判断是否收敛
      double delta = 0.0;
      for (String n : nodes) {
        delta += Math.abs(next.get(n) - pr.get(n));
      }
      pr = next;
      if (delta < tol) {
        break;
      }
    }
    return pr;
  }
  /**.
   * 执行图上的随机游走（Random Walk）操作。
   *
   * <p>从图中随机选择一个起始节点，然后按随机路径向后游走，直到遇到已访问的边或无可用出边为止。
   * 所经过的路径会被记录为一个以空格分隔的字符串，并写入文件 {@code random_walk.txt}。</p>
   *
   * @return 表示随机游走路径的字符串。如果图为空，则返回空字符串。
   * @throws RuntimeException 如果写入文件过程中发生 I/O 错误。
   */
  
  public static String randomWalk() {
    // 随机游走
    StringBuilder sb = new StringBuilder();
    Set<String> visitedEdges = new HashSet<>();
    List<String> keys = new ArrayList<>(graph.keySet());
    if (keys.isEmpty()) {
      return "";
    }
    // 随机选择起点
    String current = keys.get(random.nextInt(keys.size()));
    sb.append(current);
    while (graph.containsKey(current) && !graph.get(current).isEmpty()) {
      List<String> nextNodes = new ArrayList<>(graph.get(current).keySet());
      String next = nextNodes.get(random.nextInt(nextNodes.size()));
      String edge = current + "->" + next;
      // 遇到重复边则停止
      if (visitedEdges.contains(edge)) {
        break;
      }
      visitedEdges.add(edge);
      sb.append(" ").append(next);
      current = next;
    }
    try {
      Files.writeString(Path.of("random_walk.txt"), sb.toString());
    } catch (IOException e) {
      // 包装为未检查异常，避免修改方法签名
      throw new RuntimeException("文件写入失败", e);
    }
    return sb.toString();
  }
}