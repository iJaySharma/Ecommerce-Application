package com.pms.service;

import com.pms.dto.request.AddressRequest;
import com.pms.dto.response.AddressResponse;

import java.util.List;

public interface AddressService {
    AddressResponse addAddress(Long userId, AddressRequest request);
    List<AddressResponse> getAddresses(Long userId);
}
