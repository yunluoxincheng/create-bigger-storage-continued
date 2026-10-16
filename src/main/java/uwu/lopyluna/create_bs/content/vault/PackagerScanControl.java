package uwu.lopyluna.create_bs.content.vault;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;
import net.minecraftforge.items.IItemHandler;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Invocation-scoped permission for the conservative full-package scan shortcut. */
public final class PackagerScanControl {
    private static final ThreadLocal<RequestScope> CURRENT = new ThreadLocal<>();

    private PackagerScanControl() {}

    public static RequestScope prepare(PackagerBlockEntity owner, List<PackagingRequest> requests) {
        IItemHandler source = owner.targetInventory.getInventory();
        return new RequestScope(owner, requests, source,
            owner.getClass() == PackagerBlockEntity.class && source instanceof VaultInventoryWrapper
                && isSimpleCompleteOrder(requests));
    }

    public static RequestScope enter(RequestScope scope) {
        RequestScope previous = CURRENT.get();
        CURRENT.set(scope);
        return previous;
    }

    public static void restore(RequestScope previous) {
        if (previous == null) CURRENT.remove();
        else CURRENT.set(previous);
    }

    public static boolean permits(PackagerBlockEntity owner, List<PackagingRequest> requests, IItemHandler source) {
        RequestScope scope = CURRENT.get();
        return scope != null && scope.enabled && scope.matches(owner, requests) && scope.source == source;
    }

    public static boolean isOrdinaryStack(ItemStack stack) {
        Item item = stack.getItem();
        return !stack.isEmpty() && !stack.hasTag() && stack.getMaxStackSize() == 64
            && !(item instanceof PackageItem) && item.canFitInsideContainerItems()
            && (item.getClass() == Item.class || item instanceof BlockItem)
            && item.builtInRegistryHolder().key().location().getNamespace().equals("minecraft")
            // Forge rejects a null dispatcher when serializable capability writers exist,
            // even if they currently serialize empty data. This does not certify arbitrary
            // nonserializing providers or third-party changes to vanilla stack behavior.
            && stack.areCapsCompatible((CapabilityDispatcher) null);
    }

    private static boolean isSimpleCompleteOrder(List<PackagingRequest> requests) {
        if (requests.isEmpty()) return false;
        PackagingRequest first = requests.get(0);
        PackageOrderWithCrafts context = null;
        Map<Item, Integer> remaining = new IdentityHashMap<>();
        for (PackagingRequest request : requests) {
            if (request.getCount() <= 0 || !isOrdinaryStack(request.item())
                || request.orderId() != first.orderId() || !request.address().equals(first.address())
                || request.linkIndex() != 0 || !request.finalLink().booleanValue()
                || remaining.put(request.item().getItem(), request.getCount()) != null) return false;
            if (request.context() != null) {
                if (context != null && context != request.context()) return false;
                context = request.context();
            }
        }
        if (context == null || !context.orderedCrafts().isEmpty()) return false;
        for (BigItemStack entry : context.stacks()) {
            if (!isOrdinaryStack(entry.stack) || entry.count <= 0
                || !Integer.valueOf(entry.count).equals(remaining.remove(entry.stack.getItem()))) return false;
        }
        return remaining.isEmpty();
    }

    public static final class RequestScope {
        private final PackagerBlockEntity owner;
        private final List<PackagingRequest> requests;
        private final IItemHandler source;
        private final boolean enabled;

        private RequestScope(PackagerBlockEntity owner, List<PackagingRequest> requests, IItemHandler source, boolean enabled) {
            this.owner = owner;
            this.requests = requests;
            this.source = source;
            this.enabled = enabled;
        }

        public boolean matches(PackagerBlockEntity owner, List<PackagingRequest> requests) {
            return this.owner == owner && this.requests == requests;
        }
    }
}
