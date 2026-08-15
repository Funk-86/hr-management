package org.example.hrmanagement.config;

import lombok.Data;
import org.example.hrmanagement.common.constant.FaceConstants;
import org.example.hrmanagement.common.file.AvatarService;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Data
@Component
@ConfigurationProperties(prefix = "app.attendance")
public class AttendanceProperties {
    /** 上班时间 */
    private LocalTime workStartTime = LocalTime.of(9, 0);
    /** 下班时间 */
    private LocalTime workEndTime = LocalTime.of(18, 0);

}
