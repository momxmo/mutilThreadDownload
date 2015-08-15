package com.mo.mutilthreaddownload;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

public class Test {
    // 设置线程的数量为3
    private final static int THRED_COUNT = 3;
    final static String path = "http://188.188.7.85/kugou.exe";

    public static void main(String[] args) {

        System.out.println(getFilename());

        // 创建三个线程进行多线程的下载资源文件
        try {
            URL url = new URL(path);
            // 1.获取文件资源的连接
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            // 2.获取服务器资源文件的大小
            int length = conn.getContentLength();

            //3.在本地生成与之一样的文件，使用随机流
            RandomAccessFile raf = new RandomAccessFile(getFilename(), "rw");
            //设置文件的长度
            raf.setLength(length);

            //4.开始划分每一个线程从什么位置下载到什么位置
            int blockSize = length  / THRED_COUNT;

            //5.开启3个线程进行下载

            for (int threadId = 0; threadId < THRED_COUNT; threadId++) {

                //每个线程的下载开始位置
                int startIndex = threadId*blockSize;

                //线程下载的结束位置
                int endIndex = (threadId + 1 )*blockSize -1;

                //如果是最后一个线程，让这个线程执行后面剩余的文字资源
                if(threadId == THRED_COUNT){
                    endIndex = length - 1;
                }

                //开始线程去下载，并传递进去每一个线程下载的起始位置

                new Downloader(threadId,startIndex,endIndex).start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //获取路径中的文件名
    private static String getFilename(){

        return path.substring(path.lastIndexOf("/")+1);
    }

    //下载的线程
    static class Downloader extends Thread{
        private int startIndex;
        private int endIndex;
        private int threadId;
        private int currentPosition;
        public Downloader(int threadId,int startIndex,int endIndex) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.threadId = threadId;
            currentPosition = startIndex;
            System.out.println("线程"+threadId+"-下载的位置从：" + startIndex+" ---"+endIndex);
        }

        @Override
        public void run() {
            try{
                //先找到之前记录好的位置信息
                File  file = new File(threadId+".position");

                //判断以前曾经下载过文件，这个时候必须读取里里面的位置信息
                //(断点下载的方式：就是记录每次下载之后的位置，并实时保存到硬盘中，所以使用到了随机流进行实时写出断点下载的位置文件信息)
                if(file.exists() && file.length()>0){
                    FileInputStream fis = new FileInputStream(file);
                    BufferedReader bReader = new BufferedReader(new InputStreamReader(fis));
                    //读取线程曾经下载到的位置
                    currentPosition = Integer.parseInt(bReader.readLine());
                    System.out.println("以前有下载过文件--"+threadId +"--从"+currentPosition+"开始下载");
                    bReader.close();
                    fis.close();

                }else{
                    System.out.println("以前没有下载过文件，从头开始下载------");
                }

                URL url = new URL(path);

                HttpURLConnection conn =  (HttpURLConnection) url.openConnection();
                //conn.getInputStream()//获取这个资源对应的所有输入流
                //为了实现每一线程下载的东西都是固有的位置，所以必须告诉服务器，每一个线程从什么位置开始下载

                //设置线程下载资源的位置
                conn.setRequestProperty("Range", "bytes:"+currentPosition+"-"+endIndex);

                //获取线程访问返回的状态码    线程下载返回的是206
                int code = conn.getResponseCode();

                if(206 == code){
                    InputStream in = conn.getInputStream();
                    //写文件的时候要注意，因为已经规定了每一个线程从什么位置开始下载，所以
                    //写数据的时候必须从指定位置开始写入
                    RandomAccessFile raf = new RandomAccessFile(getFilename(), "rw");
                    raf.seek(currentPosition);


                    byte[] buffer = new byte[1024];
                    int len = 0;
                    while((len = in.read(buffer)) != -1){
                        raf.write(buffer,0,len);

                        //这几必须写在上面这句语句后面，不然会数据混乱问题出现问题
                        currentPosition  += len;

                        //必须使用随机流进行读写现在下载位置的配置信息，不然会出现各种问题，（主要是硬盘的缓存问题）
                        RandomAccessFile fos= new RandomAccessFile(threadId+".position", "rwd");
                        fos.write((currentPosition+"").getBytes());
                        fos.close();
                    }
                    System.out.println("线程"+threadId+"--已经下载结束了。。");
                    //下载完成之后，要删除（断点下载的配置文件.position）
                    //如果存在断点配置.position文件，将其删除
                    file.delete();

                    raf.close();
                    in.close();


                }


            }catch (Exception e) {
                e.printStackTrace();
            }

        }



    }
}