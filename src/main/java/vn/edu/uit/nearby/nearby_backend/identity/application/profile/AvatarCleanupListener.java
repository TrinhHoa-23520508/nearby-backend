package vn.edu.uit.nearby.nearby_backend.identity.application.profile;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import vn.edu.uit.nearby.nearby_backend.shared.storage.ImageStorage;

@Component
public class AvatarCleanupListener {

	private final ImageStorage storage;

	public AvatarCleanupListener(ImageStorage storage) {
		this.storage = storage;
	}

	@TransactionalEventListener
	void deleteDiscardedAvatar(AvatarDiscarded event) {
		storage.delete(event.objectKey());
	}

}
