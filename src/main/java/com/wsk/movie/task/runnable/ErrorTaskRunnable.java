package com.wsk.movie.task.runnable;

import com.wsk.movie.task.entity.MytaskerrorEntity;
import com.wsk.movie.task.service.MyErrorTaskRepository;
import lombok.Data;
import org.springframework.stereotype.Component;

/**
 * @DESCRIPTION :错误处理
 * @AUTHOR : WuShukai1103
 * @TIME : 2018/1/24  22:41
 */
@Component
@Data
@Deprecated
public class ErrorTaskRunnable<T> extends MyRunnable {

    private final MyErrorTaskRepository repository;

    private final MytaskerrorEntity entity;

//    @Autowired
//    public ErrorTaskRunnable(MyErrorTaskRepository repository, MytaskerrorEntity entity) {
//        this.repository = repository;
//        this.entity = entity;
//    }


    @Override
    public void run() {
        repository.save(entity);
    }
}
