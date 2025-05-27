import static org.junit.Assert.assertEquals;

import java.io.IOException;
import org.junit.Before;
import org.junit.Test;

/**
 * 针对 Lab1 类的 queryBridgeWords 方法的黑盒测试类.
 */
public class queryBridgeWords_test {
  private Lab1 lab1;
  
  /**
   * 初始化测试环境，构建有向图.
   *
   * @throws IOException 如果文件读取失败
   */
  @Before
  public void setUp() throws IOException {
    lab1 = new Lab1();
    lab1.parseTextToGraph("Test easy.txt");
  }
  
  /**
   * 测试存在桥接词的情况：scientist -> carefully -> analyzed.
   */
  @Test
  public void testBridgeWordsExistBetweenScientistAndAnalyzed() {
    String result = lab1.queryBridgeWords("scientist", "analyzed");
    assertEquals(
        "The bridge words from \"scientist\" to \"analyzed\" are: carefully.",
        result
    );
  }
  
  /**
   * 测试存在桥接词的情况：report -> and -> shared.
   */
  @Test
  public void testBridgeWordsExistBetweenReportAndShared() {
    String result = lab1.queryBridgeWords("report", "shared");
    assertEquals(
        "The bridge words from \"report\" to \"shared\" are: and.",
        result
    );
  }
  
  /**
   * 测试不存在桥接词的情况：team -> data.
   */
  @Test
  public void testNoBridgeWordsBetweenTeamAndData() {
    String result = lab1.queryBridgeWords("team", "data");
    assertEquals(
        "No bridge words from \"team\" to \"data\"!",
        result
    );
  }
  
  /**
   * 测试输入单词不存在的情况：apple -> data.
   */
  @Test
  public void testWord1NotInGraph() {
    String result = lab1.queryBridgeWords("apple", "data");
    assertEquals(
        "No \"apple\" in the graph!",
        result
    );
  }
  
  /**
   * 测试输入单词不存在的情况：data -> apple.
   */
  @Test
  public void testWord2NotInGraph() {
    String result = lab1.queryBridgeWords("data", "apple");
    assertEquals(
        "No \"apple\" in the graph!",
        result
    );
  }
  
  /**
   * 测试非法输入（含非字母字符）：123 -> data.
   */
  @Test
  public void testInvalidInputWithNonAlphabeticCharacters() {
    String result = lab1.queryBridgeWords("123", "data");
    assertEquals(
        "No \"123\" in the graph!",
        result
    );
  }
}