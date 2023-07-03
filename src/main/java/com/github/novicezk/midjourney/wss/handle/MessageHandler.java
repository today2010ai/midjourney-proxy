package com.github.novicezk.midjourney.wss.handle;

import cn.hutool.core.text.CharSequenceUtil;
import com.github.novicezk.midjourney.Constants;
import com.github.novicezk.midjourney.ProxyProperties;
import com.github.novicezk.midjourney.enums.MessageType;
import com.github.novicezk.midjourney.support.DiscordHelper;
import com.github.novicezk.midjourney.support.Task;
import com.github.novicezk.midjourney.support.TaskQueueHelper;
import com.github.novicezk.midjourney.util.OssUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
public abstract class MessageHandler {
	@Resource
	protected TaskQueueHelper taskQueueHelper;
	@Resource
	protected DiscordHelper discordHelper;
	@Value("${aliyun.oss.bucket-name}")
	private String bucketName;


	public abstract void handle(MessageType messageType, DataObject message);

	public abstract void handle(MessageType messageType, Message message);

	protected String getMessageContent(DataObject message) {
		return message.hasKey("content") ? message.getString("content") : "";
	}

	protected void finishTask(Task task, DataObject message) {
		task.setProperty(Constants.TASK_PROPERTY_MESSAGE_ID, message.getString("id"));
		task.setProperty(Constants.TASK_PROPERTY_FLAGS, message.getInt("flags", 0));
		DataArray attachments = message.getArray("attachments");
		if (!attachments.isEmpty()) {
			String imageUrl = attachments.getObject(0).getString("url");
			task.setImageUrl(replaceOssUrl(imageUrl));
			task.setProperty(Constants.TASK_PROPERTY_MESSAGE_HASH, getMessageHash(imageUrl));
			task.success();
		} else {
			task.fail("关联图片不存在");
		}
	}

	protected void finishTask(Task task, Message message) {
		task.setProperty(Constants.TASK_PROPERTY_MESSAGE_ID, message.getId());
		task.setProperty(Constants.TASK_PROPERTY_FLAGS, (int) message.getFlagsRaw());
		if (!message.getAttachments().isEmpty()) {
			String imageUrl = message.getAttachments().get(0).getUrl();
			task.setImageUrl(replaceOssUrl(imageUrl));
			task.setProperty(Constants.TASK_PROPERTY_MESSAGE_HASH, getMessageHash(imageUrl));
			task.success();
		} else {
			task.fail("关联图片不存在");
		}
	}

	protected String getMessageHash(String imageUrl) {
		int hashStartIndex = imageUrl.lastIndexOf("_");
		return CharSequenceUtil.subBefore(imageUrl.substring(hashStartIndex + 1), ".", true);
	}

	protected String getImageUrl(DataObject message) {
		DataArray attachments = message.getArray("attachments");
		if (!attachments.isEmpty()) {
			String imageUrl = attachments.getObject(0).getString("url");
			return replaceCdnUrl(imageUrl);
		}
		return null;
	}

	protected String getImageUrl(Message message) {
		if (!message.getAttachments().isEmpty()) {
			String imageUrl = message.getAttachments().get(0).getUrl();
			return replaceCdnUrl(imageUrl);
		}
		return null;
	}

	protected String replaceCdnUrl(String imageUrl) {
		if (CharSequenceUtil.isBlank(imageUrl)) {
			return imageUrl;
		}
		String cdn = this.discordHelper.getCdn();
		if (CharSequenceUtil.startWith(imageUrl, cdn)) {
			return imageUrl;
		}
		return CharSequenceUtil.replaceFirst(imageUrl, DiscordHelper.DISCORD_CDN_URL, cdn);
	}

	protected String replaceOssUrl(String imageUrl) {
		if (CharSequenceUtil.isBlank(imageUrl)) {
			return imageUrl;
		}
		log.debug("imageUrl: {}", imageUrl);
		if(imageUrl.startsWith(DiscordHelper.DISCORD_CDN_URL)) {
			String path = imageUrl.substring(DiscordHelper.DISCORD_CDN_URL.length()+1);
			try {
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
				String yyyymmdd = dateFormat.format(new Date());
				path = CharSequenceUtil.replaceFirst(path, "attachments/", "attachments/"+yyyymmdd+"/");
				long begin = System.currentTimeMillis();
				log.debug("upload begin: {}", path);
				imageUrl = OssUtils.uploadFileByUrl(imageUrl, bucketName, "jiandanai", path);
				log.debug("upload end url:{} time:{}", System.currentTimeMillis()-begin,imageUrl);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return imageUrl;
	}

}
