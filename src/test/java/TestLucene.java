import com.dfbz.entity.Good;
import com.dfbz.entity.com.dfbz.dao.GoodDao;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.junit.Test;
import org.wltea.analyzer.lucene.IKAnalyzer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestLucene {


    @Test
    public void testFindAll() {
        GoodDao goodDao = new GoodDao();
        List<Good> all = goodDao.findAll();
        for (Good good : all) {
            System.out.println(good);
        }
    }

    @Test
    public void testCreateIndex() throws IOException {
        //1.读取原始数据（从数据库读取）
        GoodDao goodDao = new GoodDao();
        List<Good> all = goodDao.findAll();

        //2.创建文档对象(Document)、域对象(Field)。并把域对象添加到文档对象中
        ArrayList<Document> docs = new ArrayList<Document>();
        for (Good good : all) {
            Document doc = new Document();

            Integer id = good.getId();
            String name = good.getName();
            String title = good.getTitle();
            Double price = good.getPrice();
            String pic = good.getPic();

            StringField _id = new StringField("id", id + "", Field.Store.YES);
            TextField _name = new TextField("name", name, Field.Store.YES);
            TextField _title = new TextField("title", title, Field.Store.YES);
            DoubleField _price = new DoubleField("price", price, Field.Store.YES);
            StoredField _pic = new StoredField("pic", pic);

            doc.add(_id);
            doc.add(_name);
            doc.add(_title);
            doc.add(_price);
            doc.add(_pic);

            docs.add(doc);
        }

        //3.创建分线器（Analyzer）,用于分词
//        StandardAnalyzer analyzer = new StandardAnalyzer();//一元切分
//        CJKAnalyzer analyzer = new CJKAnalyzer();//二元切分
        IKAnalyzer analyzer = new IKAnalyzer();

        //4.创建索引库配置对象（IndexWriterConfig）,配置索引库（传入分析器）
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_10_3, analyzer);

        /**
         * 5.设置索引库打开方式（OpenModel）
         *  CREATE:每次运行都会把原来的删除,新创建一个
         *
         *  APPEND:每次运行都追加到索引库中
         *
         *  CREATE_OR_APPEND:如果有索引库那么则增加,没有索引库则创建
         */
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        //6.创建索引库目录对象（Directory）,指定索引库的目录
        Directory dir = FSDirectory.open(new File("D://index"));

        //7.创建索引库操作对象（IndexWriter）,用于把文档写入索引库中
        IndexWriter writer = new IndexWriter(dir, config);

        for (Document doc : docs) {
            writer.addDocument(doc);
        }

        //8.提交事务（commit）
        writer.commit();
        //9.释放资源
        writer.close();

    }


    @Test
    public void testDeleteIndex() throws IOException {


        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_10_3, new IKAnalyzer());
        FSDirectory dir = FSDirectory.open(new File("D://index"));
        IndexWriter writer = new IndexWriter(dir, config);
//        Term term = new Term("id","1");
//        writer.deleteDocuments(term);
        writer.deleteAll();
        writer.commit();
        writer.close();

    }

    @Test
    public void testUpdatIndex() throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_10_3, new IKAnalyzer());
        FSDirectory dir = FSDirectory.open(new File("D://index"));
        IndexWriter writer = new IndexWriter(dir, config);
        Document doc = new Document();
        doc.add(new StringField("id", "30", Field.Store.YES));
        doc.add(new StringField("title", "修改后的手机", Field.Store.YES));

        Term term = new Term("title", "华为");

        /**
         * 把通过term搜索到的文档都先删除,然后再添加一篇新文档,如果一篇也没有搜索到那么则添加doc文档
         */
        writer.updateDocument(term, doc);

        writer.commit();
        writer.close();
    }

    @Test
    public void testQuery() throws Exception {

        //1.创建索引库目录对象，指定索引库目录
        FSDirectory dir = FSDirectory.open(new File("D://index"));

        //2.创建索引库搜索对象（IndexReader）,指定把索引库数据库读取到内存中
        DirectoryReader reader = DirectoryReader.open(dir);

        //3.创建索引库搜索对象（IndexSearcher）,用于搜索索引库
        IndexSearcher searcher = new IndexSearcher(reader);

        //4.创建分词器，用于搜索条件分词
        IKAnalyzer analyzer = new IKAnalyzer();

        //5.创建查询解析器（QueryParse）,传入分词器并指定查询的域
        QueryParser queryParser = new QueryParser(Version.LUCENE_4_10_3, "name", analyzer);

        //6.创建查询对象（Query）,指定查询条件
        Query query = queryParser.parse("name:东标方准");

        //7.使用索引库搜索对象（IndexSearcher）执行搜索，返回搜索结果（TopDocs）
        TopDocs topDocs = searcher.search(query, 100);

        //8.处理结果集
        System.out.println("topDocs:" + topDocs);
        System.out.println("返回查询的条数：" + topDocs.totalHits);

        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        print(scoreDocs, searcher);
        reader.close();

    }


    @Test
    public void testQueryIndex() throws Exception {
        FSDirectory dir = FSDirectory.open(new File("D://index"));
        DirectoryReader reader = DirectoryReader.open(dir);
        IndexSearcher searcher = new IndexSearcher(reader);
        IKAnalyzer analyzer = new IKAnalyzer(true);

        QueryParser queryParser = new QueryParser(Version.LUCENE_4_10_3, "title", analyzer);
        Query query = queryParser.parse("title:手机");
        TopDocs topDocs = searcher.search(query, 10);
        System.out.println("查询结果条数：" + topDocs.totalHits);
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;

        print(scoreDocs, searcher);
        reader.close();

    }

    @Test
    public void testTermQuery() throws Exception {
        FSDirectory dir = FSDirectory.open(new File("D://index"));
        DirectoryReader reader = DirectoryReader.open(dir);
        IndexSearcher searcher = new IndexSearcher(reader);

        TermQuery termQuery = new TermQuery(new Term("id", "2"));
        TopDocs topDocs = searcher.search(termQuery, 10);
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        print(scoreDocs, searcher);

        reader.close();
    }

    @Test
    public void numericRangeQuery() throws Exception {
        FSDirectory dir = FSDirectory.open(new File("D://index"));
        DirectoryReader reader = DirectoryReader.open(dir);
        IndexSearcher searcher = new IndexSearcher(reader);

        NumericRangeQuery<Double> rangeQuery = NumericRangeQuery.newDoubleRange("price", 5199D, 6000D, false, true);
        TopDocs topDocs = searcher.search(rangeQuery, 10);
        System.out.println("查询的语法：" + rangeQuery);
        System.out.println("查询总条数：" + topDocs.totalHits);
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        print(scoreDocs, searcher);

        reader.close();
    }

    @Test
    public void booleanQuery() throws Exception {
        FSDirectory dir = FSDirectory.open(new File("D://index"));
        DirectoryReader reader = DirectoryReader.open(dir);
        IndexSearcher searcher = new IndexSearcher(reader);

        BooleanQuery booleanQuery = new BooleanQuery();
        Query q1 = new TermQuery(new Term("id","11"));
        Query q2 = NumericRangeQuery.newDoubleRange("price", 5199D, 6000D, false, true);

        booleanQuery.add(q1, BooleanClause.Occur.MUST);
        booleanQuery.add(q2, BooleanClause.Occur.MUST);


        TopDocs topDocs = searcher.search(booleanQuery, 10);
        System.out.println("查询的语法：" + booleanQuery);
        System.out.println("查询总条数：" + topDocs.totalHits);
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        print(scoreDocs, searcher);

        reader.close();
    }


    public void print(ScoreDoc[] scoreDocs, IndexSearcher searcher) throws Exception {

        for (ScoreDoc scoreDoc : scoreDocs) {

            System.out.println("文档id: " + scoreDoc.doc + ";文档分数: " + scoreDoc.score);

            //根据文档id搜索出对应文档
            Document doc = searcher.doc(scoreDoc.doc);

            String id = doc.get("id");
            String name = doc.get("name");
            String title = doc.get("title");
            String price = doc.get("price");
            String pic = doc.get("pic");

            System.out.println("商品id: " + id);
            System.out.println("商品名称: " + name);
            System.out.println("商品标题: " + title);
            System.out.println("商品价格: " + price);
            System.out.println("商品图片: " + pic);
            System.out.println("-------------------");

        }
    }

}
