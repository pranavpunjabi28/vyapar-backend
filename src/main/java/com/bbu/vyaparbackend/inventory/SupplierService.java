package com.bbu.vyaparbackend.inventory;

import com.bbu.vyaparbackend.business.Outlet;
import com.bbu.vyaparbackend.shared.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static com.bbu.vyaparbackend.shared.Pageables.requireAllowedSort;

@Service
public class SupplierService {
    private final SupplierRepository suppliers;

    SupplierService(SupplierRepository suppliers) {
        this.suppliers = suppliers;
    }

    @Transactional(readOnly = true)
    public Page<Supplier> list(Outlet outlet, String search, Pageable pageable) {
        pageable = requireAllowedSort(pageable, Set.of("id", "name", "createdAt"), Sort.by("name"));
        return suppliers.findAllByOutletIdAndArchivedFalseAndNameContainingIgnoreCase(outlet.getId(),
                search == null ? "" : search, pageable);
    }

    @Transactional
    public Supplier save(Outlet outlet, String supplierId, InventoryCommands.SupplierData command) {
        Supplier supplier = supplierId == null ? new Supplier() : require(outlet, supplierId);
        supplier.setOutlet(outlet);
        supplier.setName(command.name());
        supplier.setPhone(command.phone());
        supplier.setEmail(command.email());
        supplier.setGstin(command.gstin());
        supplier.setAddress(command.address());
        return suppliers.save(supplier);
    }

    @Transactional(readOnly = true)
    public Supplier require(Outlet outlet, String supplierId) {
        return suppliers.findById(supplierId)
                .filter(supplier -> !supplier.isArchived() && supplier.getOutlet().getId().equals(outlet.getId()))
                .orElseThrow(() -> ApiException.notFound("Supplier"));
    }

    @Transactional
    public void archive(Outlet outlet, String supplierId) {
        require(outlet, supplierId).setArchived(true);
    }
}
