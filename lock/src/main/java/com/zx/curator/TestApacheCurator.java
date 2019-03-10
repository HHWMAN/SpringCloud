package com.zx.curator;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.BackgroundCallback;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.framework.api.CuratorEventType;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestApacheCurator {

    //会话超时时间
    private final int SESSION_TIMEOUT = 30 * 1000;

    //连接超时时间
    private final int CONNECTION_TIMEOUT = 3 * 1000;

    //ZooKeeper服务地址
    private static final String SERVER = "localhost:2181";

    //创建连接实例
    private CuratorFramework client = null;

    /**
     * baseSleepTimeMs：初始的重试等待时间
     * maxRetries：最多重试次数
     */
    RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);

    @Before
    public void init(){
        //创建 CuratorFrameworkImpl实例
        client = CuratorFrameworkFactory.newClient(SERVER, SESSION_TIMEOUT, CONNECTION_TIMEOUT, retryPolicy);

        //启动
        client.start();
    }

    /**
     * 测试创建节点
     * @throws Exception
     */
    @Test
    public void testCreate() throws Exception{
        //创建永久节点
        client.create().forPath("/curator","/curator data".getBytes());

        //创建永久有序节点
        client.create().withMode(CreateMode.PERSISTENT_SEQUENTIAL).forPath("/curator_sequential","/curator_sequential data".getBytes());

        //创建临时节点
        client.create().withMode(CreateMode.PERSISTENT)
                .forPath("/curator/ephemeral","/curator/ephemeral data".getBytes());

        //创建临时有序节点
        client.create().withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                .forPath("/curator/ephemeral_path1","/curator/ephemeral_path1 data".getBytes());

        client.create().withProtection().withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                .forPath("/curator/ephemeral_path2","/curator/ephemeral_path2 data".getBytes());
    }

    @Test
    public  void createNode(){
        String path = "/curator/test01";
        String value = "curator-test01";
        try {
            //同步方式创建
            client.create()
                    .creatingParentsIfNeeded()  //如果父节点不存在，则自动创建
                    .withMode(CreateMode.PERSISTENT)  //节点模式，持久
                    .forPath(path, value.getBytes());

            //	client.create()
            //     .creatingParentsIfNeeded()  //如果父节点不存在，则自动创建
            //     .withMode(CreateMode.EPHEMERAL)  //节点模式，临时
            //     .inBackground()//后台方式，即异步方式创建
            //    .forPath(path, value.getBytes(defaultCharSet));


        }  catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public  void update(){
        try {
            String path = "/curator/test01";
            String value = "curator-test02";

            //先获取节点状态信息
            Stat stat = new Stat();
            //获取节点值，并同时获取节点状态信息
            byte[] data = client.getData().storingStatIn(stat).forPath(path);
            //更新节点
            client.setData()
                    .withVersion(stat.getVersion())  //版本校验，与当前版本不一致则更新失败,默认值-1无视版本信息进行更新
                    //  .inBackground(paramBackgroundCallback)  //异步修改数据，并进行回调通知
                    .forPath(path, value.getBytes());
            data = client.getData().forPath(path);
            System.out.println("修改后的值=" + new String(data));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public  void delete(){
        try {
            //删除已存在的节点
            String path = "/curator/test01";
            client.delete()
                    .guaranteed()  //删除失败，则客户端持续删除，直到节点删除为止
                    .deletingChildrenIfNeeded()  //删除相关子节点
                    .withVersion(-1) //无视版本，直接删除
                    .forPath(path);
            //删除不存在的节点
            String path2 = "/curator/test02";
            client.delete()
                    //       .guaranteed()  //删除失败，则客户端持续删除，直到节点删除为止
                    .deletingChildrenIfNeeded()  //删除相关子节点
                    .withVersion(-1) //无视版本，直接删除
                    .forPath(path2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //判断是否存在
    @Test
    public  void checkExist(){
        try {
            String path = "/curator";
            Stat stat = client.checkExists().forPath(path);
            if(stat == null){
                System.out.println("节点" + path + "不存在");
            }else{
                System.out.println("节点" + path + "已存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //回调
    @Test
    public  void callBack( ){
        try {
            String path = "/curator/test01";
            byte[] data = null;
            //同步获取
            data = client.getData().forPath(path);//同步
            System.out.println("同步方式获取节点数据，data=" + (data == null ? "null" : new String(data)));
            //无回调的异步获取
            data = client.getData().inBackground().forPath(path);//无回调的异步
            //data可能是空，无法预测
            System.out.println("无回调的异步方式获取节点数据，data=" + (data == null ? "null" : new String(data)));
            //有回调通知的异步获取
            //异步操作线程池,避免线程过多，给服务器造成影响。。。。使用线程池，要考虑线程池的关闭（这里省略）
            ExecutorService es = Executors.newFixedThreadPool(5);
            data = client.getData().inBackground(new BackgroundCallback() {
                public void processResult(CuratorFramework curatorFramework, CuratorEvent curatorEvent) throws Exception {
                    CuratorEventType c = curatorEvent.getType();//事件类型，可在CuratorEventType看到具体种类
                    int r = curatorEvent.getResultCode();//0,执行成功，其它，执行失败
                    Object o = curatorEvent.getContext();//事件上下文，一般是由调用方法传入，供回调函数使用的参数
                    String p = curatorEvent.getPath();//节点路径
                    List<String> li = curatorEvent.getChildren();//子节点列表
                    byte[] _data = curatorEvent.getData();//节点数据
                    System.out.println("回调通知，data=" + (_data == null ? "null" : new String(_data)));
                }
            },es).forPath(path);//有回调的异步
            System.out.println("有回调的异步方式获取节点数据，data=" + (data == null ? "null" : new String(data)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 节点监听需要用repices包中的NodeCache来完成
     * 子节点的监听需要用PathChildrenCache来完成
     * @param client
     */
    @Test
    public  void nodeMonitor( ){
        try {
            //节点监听需要用repices包中的NodeCache来完成
            final String path = "/curator/test01";
            final NodeCache cache = new NodeCache(client,path);
            cache.start();
            cache.getListenable().addListener(new NodeCacheListener() {
                public void nodeChanged() throws Exception {
                    byte[] ret = cache.getCurrentData().getData();
                    System.out.println("当前节点" + path +"=："+ new String(ret));
                }
            });
//			cache.close();
            //在父节点进行监听
            final String pPath = "/curator";
            PathChildrenCache pcCache = new PathChildrenCache(client,pPath,true);
            pcCache.start();
            pcCache.getListenable().addListener(new PathChildrenCacheListener() {
                public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent)
                        throws Exception {
                    switch (pathChildrenCacheEvent.getType()){//子节点的事件类型
                        //通过pathChildrenCacheEvent，可以获取到节点相关的数据
                        case CHILD_ADDED:
                            System.out.println("增加节点" + pathChildrenCacheEvent.getData().getPath()
                                    + "=" +new String(pathChildrenCacheEvent.getData().getData()) );
                            break;
                        case CHILD_REMOVED:
                            System.out.println("删除节点"+pathChildrenCacheEvent.getData().getPath());
                            break;
                        case CHILD_UPDATED:
                            System.out.println("更新节点" + pathChildrenCacheEvent.getData().getPath()
                                    + "=" +new String(pathChildrenCacheEvent.getData().getData()) );
                            break;
                        default:
                            break;
                    }

                }
            });
            //		pcCache.close();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //事务
    @Test
    public  void tansation(){
        try {
            client.inTransaction()
                    .create().withMode(CreateMode.EPHEMERAL).forPath("/t01","ttt".getBytes())
                    .and()
                    .setData().withVersion(-1).forPath("/conf/curator/test01", "fei002".getBytes())
                    .and()
                    .delete().withVersion(-1).forPath("/curator/test01")
                    .and()
                    .commit();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}
