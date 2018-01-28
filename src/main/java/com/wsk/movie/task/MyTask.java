package com.wsk.movie.task;

import com.wsk.movie.task.entity.MytaskEntity;
import com.wsk.movie.task.entity.MytaskerrorEntity;
import com.wsk.movie.task.entity.MytasklogEntity;
import com.wsk.movie.task.runnable.MyQueue;
import com.wsk.movie.task.runnable.MyQueueBean;
import com.wsk.movie.task.runnable.MyRunnable;
import com.wsk.movie.task.service.MyErrorTaskRepository;
import com.wsk.movie.task.service.MyTaskLogRepository;
import com.wsk.movie.task.service.MyTaskRepository;
import com.wsk.movie.task.tool.TimeTransform;
import com.wsk.movie.tool.SpringContextUtil;

import java.sql.Timestamp;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @DESCRIPTION :自定义定时器-单例
 * @AUTHOR : WuShukai1103
 * @TIME : 2018/1/23  22:22
 */
public class MyTask implements Runnable{
    /**
     * 使用定时线程池
     * 根据CPU进行任务调度
     */
    private static ScheduledExecutorService service = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
    private static MyErrorTaskRepository errorTaskRepository = (MyErrorTaskRepository) SpringContextUtil.getBean(MyErrorTaskRepository.class);
    private static MyTaskLogRepository logRepository = (MyTaskLogRepository) SpringContextUtil.getBean(MyTaskLogRepository.class);
    private static MyTaskRepository repository = (MyTaskRepository) SpringContextUtil.getBean(MyTaskRepository.class);

    private MyTask() {
    }

    @Override
    public void run() {
        execute();
    }

    private static class NestClass {
        private static final MyTask MY_TASK = new MyTask();
    }

    public static MyTask getInstance() {
        return NestClass.MY_TASK;
    }

    /**
     * 时间表达式
     * 1:00 00 00 00-中间以空格分开
     * :秒 分 时 日
     * 2:yyyy-MM-dd HH:mm:ss
     * 3:yyyy-MM-dd
     *
     * @param runnable
     * @param startTime
     * @param endTime
     */
    public void execute(MyRunnable runnable, String startTime, String endTime) {
        long start = TimeTransform.getTime(startTime);
        long end = TimeTransform.getTime(endTime);
        service.scheduleAtFixedRate(runnable, start, end, TimeUnit.SECONDS);
    }

    public void execute(MyRunnable runnable, Date startTime, Date endTime) {
        long start = TimeTransform.getTime(startTime);
        long end = TimeTransform.getTime(endTime);
        service.scheduleAtFixedRate(runnable, start, end, TimeUnit.SECONDS);
    }

//    public void execute(MyRunnable runnable) {
//        service.scheduleAtFixedRate(runnable, start, end, TimeUnit.SECONDS);
//    }

    /**
     * 默认现在开始运行
     *
     * @param runnable
     * @param endTime
     */
    public void execute(Runnable runnable, String endTime) {
        long end = TimeTransform.getTime(endTime);
        service.scheduleAtFixedRate(runnable, 0, end, TimeUnit.SECONDS);
    }

    public void execute(MyQueue queue) {
        MyQueueBean bean;
        Date now = new Date();
        try {
            bean = queue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
            MytaskerrorEntity entity = new MytaskerrorEntity();
            entity.setTaskname("");
            entity.setMsg("队列获取失败");
            entity.setRtime(new Timestamp(now.getTime()));
            entity.setClassname("");
            errorTaskRepository.save(entity);
            return;
        }
        MyRunnable runnable = bean.getRunnable();
        MytaskEntity entity = bean.getEntity();
        //更新数据库
        long next = TimeTransform.getTime(entity.getExpression());
        repository.updateTime(entity.getTaskname(), new Timestamp(now.getTime()), new Timestamp(now.getTime() + next * 1000));
        service.scheduleAtFixedRate(runnable, TimeTransform.getTime(entity.getStarttime()), next, TimeUnit.SECONDS);
        //日志
        MytasklogEntity log = new MytasklogEntity();
        log.setTaskname(entity.getTaskname());
        log.setClassname(entity.getClassname());
        log.setRtime(new Timestamp(now.getTime()));
        logRepository.save(log);
    }


    public void execute() {
        while (true) {
            System.out.println("开始定时任务,size:" + MyQueue.getInstance().size());
            execute(MyQueue.getInstance());
        }
    }

//    private void saveOrUpdate(MyRunnable runnable) {
//        MytaskEntity bean = runnable.getEntity();
//        bean = repository.findByTaskname(bean.getTaskname());
//        if (Tool.getInstance().isNullOrEmpty(bean)) {
//            repository.save(bean);
//        } else {
////            repository.updateTime(runnable.getEntity().getTaskname(), runnable.getEntity().getStarttime(),runnable.getEntity().getNexttime());
//        }
//    }

    public void shutdown() {
        service.shutdown();
    }

}
